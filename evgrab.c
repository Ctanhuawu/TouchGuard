#include <fcntl.h>
#include <unistd.h>
#include <linux/input.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <dirent.h>
#include <sys/poll.h>
#include <sys/time.h>

#define BITS_PER_LONG (sizeof(long) * 8)
#define NBITS(x) ((((x)-1)/BITS_PER_LONG)+1)
#define OFF(x)  ((x)%BITS_PER_LONG)
#define BIT(x)  (1UL<<OFF(x))
#define LONG(x) ((x)/BITS_PER_LONG)
#define test_bit(bit, array) ((array[LONG(bit)] >> OFF(bit)) & 1)

#ifndef BTN_TOUCH
#define BTN_TOUCH 0x14a
#endif
#ifndef BTN_TOOL_PEN
#define BTN_TOOL_PEN 0x140
#endif
#ifndef ABS_MT_POSITION_X
#define ABS_MT_POSITION_X 0x35
#endif

#define MAX_TOUCH_FDS 4
#define MAX_KEY_FDS 8

static int g_touch_fds[MAX_TOUCH_FDS];
static char g_touch_paths[MAX_TOUCH_FDS][64];
static int g_num_touch_fds = 0;

static int g_key_fds[MAX_KEY_FDS];
static int g_num_key_fds = 0;

void handle_signal(int sig) {
    // 立即释放所有触控屏及手写笔的硬件独占
    for (int i = 0; i < g_num_touch_fds; i++) {
        if (g_touch_fds[i] >= 0) {
            ioctl(g_touch_fds[i], EVIOCGRAB, 0);
            close(g_touch_fds[i]);
            g_touch_fds[i] = -1;
        }
    }
    for (int i = 0; i < g_num_key_fds; i++) {
        if (g_key_fds[i] >= 0) {
            close(g_key_fds[i]);
            g_key_fds[i] = -1;
        }
    }
    _exit(0);
}

static long long get_time_ms(void) {
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return ((long long)tv.tv_sec) * 1000 + (tv.tv_usec / 1000);
}

static int is_touch_device(int fd) {
    unsigned long evbit[NBITS(EV_MAX)];
    memset(evbit, 0, sizeof(evbit));
    if (ioctl(fd, EVIOCGBIT(0, sizeof(evbit)), evbit) < 0) return 0;
    if (!test_bit(EV_ABS, evbit) && !test_bit(EV_KEY, evbit)) return 0;

    unsigned long keybit[NBITS(KEY_MAX)];
    memset(keybit, 0, sizeof(keybit));
    ioctl(fd, EVIOCGBIT(EV_KEY, sizeof(keybit)), keybit);

    unsigned long absbit[NBITS(ABS_MAX)];
    memset(absbit, 0, sizeof(absbit));
    ioctl(fd, EVIOCGBIT(EV_ABS, sizeof(absbit)), absbit);

    // 多点触控屏幕（具备 ABS_MT_POSITION_X）或触控手势（BTN_TOUCH / BTN_TOOL_PEN）
    if (test_bit(ABS_MT_POSITION_X, absbit)) return 1;
    if (test_bit(BTN_TOUCH, keybit) || test_bit(BTN_TOOL_PEN, keybit)) return 1;

    return 0;
}

static void discover_and_grab_touch_devices(const char *specified_dev) {
    // 若用户显式指定了设备路径且非 auto，优先尝试
    if (specified_dev && strcmp(specified_dev, "auto") != 0 && strncmp(specified_dev, "/dev/input/", 11) == 0) {
        int fd = open(specified_dev, O_RDONLY);
        if (fd >= 0) {
            if (ioctl(fd, EVIOCGRAB, 1) >= 0) {
                g_touch_fds[g_num_touch_fds] = fd;
                strncpy(g_touch_paths[g_num_touch_fds], specified_dev, 63);
                g_num_touch_fds++;
                return;
            }
            close(fd);
        }
    }

    // 动态扫描 /dev/input/ 下所有节点，自动识别触控屏与触控笔
    DIR *dir = opendir("/dev/input");
    if (!dir) return;

    struct dirent *ent;
    while ((ent = readdir(dir)) != NULL) {
        if (strncmp(ent->d_name, "event", 5) != 0) continue;
        char path[64];
        snprintf(path, sizeof(path), "/dev/input/%s", ent->d_name);

        int fd = open(path, O_RDONLY);
        if (fd < 0) continue;

        if (is_touch_device(fd)) {
            if (ioctl(fd, EVIOCGRAB, 1) >= 0) {
                g_touch_fds[g_num_touch_fds] = fd;
                strncpy(g_touch_paths[g_num_touch_fds], path, 63);
                g_num_touch_fds++;
                if (g_num_touch_fds >= MAX_TOUCH_FDS) break;
            } else {
                close(fd);
            }
        } else {
            close(fd);
        }
    }
    closedir(dir);
}

static int is_grabbed_touch_dev(const char *path) {
    for (int i = 0; i < g_num_touch_fds; i++) {
        if (strcmp(g_touch_paths[i], path) == 0) return 1;
    }
    return 0;
}

static void scan_key_devices(void) {
    DIR *dir = opendir("/dev/input");
    if (!dir) return;

    struct dirent *ent;
    while ((ent = readdir(dir)) != NULL) {
        if (strncmp(ent->d_name, "event", 5) != 0) continue;
        char path[64];
        snprintf(path, sizeof(path), "/dev/input/%s", ent->d_name);
        if (is_grabbed_touch_dev(path)) continue;

        int fd = open(path, O_RDONLY | O_NONBLOCK);
        if (fd < 0) continue;

        unsigned long keybit[NBITS(KEY_MAX)];
        memset(keybit, 0, sizeof(keybit));
        if (ioctl(fd, EVIOCGBIT(EV_KEY, sizeof(keybit)), keybit) >= 0) {
            if (test_bit(KEY_VOLUMEDOWN, keybit) || test_bit(KEY_VOLUMEUP, keybit)) {
                if (g_num_key_fds < MAX_KEY_FDS) {
                    g_key_fds[g_num_key_fds++] = fd;
                } else {
                    close(fd);
                }
                continue;
            }
        }
        close(fd);
    }
    closedir(dir);
}

enum UnlockMode {
    MODE_DOUBLE_VOL_DOWN = 0,
    MODE_TRIPLE_VOL_DOWN,
    MODE_DOUBLE_VOL_UP,
    MODE_VOL_UP_THEN_DOWN,
    MODE_VOL_DOWN_THEN_UP
};

int main(int argc, char *argv[]) {
    const char *dev = "auto";
    if (argc > 1 && argv[1][0] != '\0') {
        dev = argv[1];
    }

    enum UnlockMode mode = MODE_DOUBLE_VOL_DOWN;
    if (argc > 2) {
        if (strcmp(argv[2], "triple_volume_down") == 0) {
            mode = MODE_TRIPLE_VOL_DOWN;
        } else if (strcmp(argv[2], "double_volume_up") == 0) {
            mode = MODE_DOUBLE_VOL_UP;
        } else if (strcmp(argv[2], "volume_up_then_down") == 0) {
            mode = MODE_VOL_UP_THEN_DOWN;
        } else if (strcmp(argv[2], "volume_down_then_up") == 0) {
            mode = MODE_VOL_DOWN_THEN_UP;
        }
    }

    signal(SIGTERM, handle_signal);
    signal(SIGINT, handle_signal);
    signal(SIGHUP, handle_signal);
    signal(SIGQUIT, handle_signal);
    signal(SIGPIPE, handle_signal);

    // 动态探测并独占所有触控设备（屏幕 + 触控笔）
    discover_and_grab_touch_devices(dev);
    if (g_num_touch_fds == 0) {
        fprintf(stderr, "Failed to find or grab any touch devices\n");
        return 1;
    }

    // 扫描物理按键输入设备节点（自动识别音量键）
    scan_key_devices();

    printf("GRAB_SUCCESS touch_devs=%d key_devs=%d\n", g_num_touch_fds, g_num_key_fds);
    fflush(stdout);

    // 监听 STDIN（父进程心跳管道）和所有实体按键输入设备
    struct pollfd pfd[MAX_KEY_FDS + 1];
    pfd[0].fd = STDIN_FILENO;
    pfd[0].events = POLLIN | POLLHUP | POLLERR;

    for (int i = 0; i < g_num_key_fds; i++) {
        pfd[i + 1].fd = g_key_fds[i];
        pfd[i + 1].events = POLLIN;
    }

    long long last_press_time = 0;
    int press_count = 0;
    int last_key_code = 0;
    int is_voldown_pressed = 0;
    int is_volup_pressed = 0;
    int is_long_press_suppressed = 0;

    while (1) {
        int ret = poll(pfd, g_num_key_fds + 1, -1);
        if (ret < 0) break;

        // 管道自毁心跳监视：TouchGuard 主进程退出/被杀/主动请求解除
        if (pfd[0].revents & (POLLIN | POLLHUP | POLLERR)) {
            char buf[64];
            ssize_t n = read(STDIN_FILENO, buf, sizeof(buf));
            if (n <= 0) break;
            if (n >= 4 && (buf[0] == 'Q' || buf[0] == 'q')) break;
        }

        // 实体按键事件检测
        for (int i = 0; i < g_num_key_fds; i++) {
            if (pfd[i + 1].revents & POLLIN) {
                struct input_event ev;
                while (read(g_key_fds[i], &ev, sizeof(ev)) == sizeof(ev)) {
                    if (ev.type != EV_KEY) continue;

                    int code = ev.code;
                    int val = ev.value;
                    long long now = get_time_ms();

                    // 1. Linux 内核按键自动重复 (value == 2)，说明按键正在持续长按，标记并丢弃
                    if (val == 2) {
                        is_long_press_suppressed = 1;
                        continue;
                    }

                    // 2. 按键释放事件 (value == 0)
                    if (val == 0) {
                        if (code == KEY_VOLUMEDOWN) {
                            is_voldown_pressed = 0;
                        } else if (code == KEY_VOLUMEUP) {
                            is_volup_pressed = 0;
                        }
                        is_long_press_suppressed = 0;
                        continue;
                    }

                    // 3. 仅响应实体按键按下 (value == 1)
                    if (val == 1) {
                        // 如果该键之前已经处于按下状态（未收到释放事件），说明是长按/多节点并发，坚决忽略
                        if (code == KEY_VOLUMEDOWN) {
                            if (is_voldown_pressed) {
                                is_long_press_suppressed = 1;
                                continue;
                            }
                            is_voldown_pressed = 1;
                        } else if (code == KEY_VOLUMEUP) {
                            if (is_volup_pressed) {
                                is_long_press_suppressed = 1;
                                continue;
                            }
                            is_volup_pressed = 1;
                        }

                        if (is_long_press_suppressed) {
                            continue;
                        }

                        // 物理去抖动与最小按键间隔（人类不可能在 180ms 内完成两次独立完整物理点击）
                        if (now - last_press_time < 180) {
                            continue;
                        }

                        if (mode == MODE_DOUBLE_VOL_DOWN) {
                            if (code == KEY_VOLUMEDOWN) {
                                if (now - last_press_time < 1000) {
                                    printf("UNLOCKED:double_volume_down\n");
                                    fflush(stdout);
                                    handle_signal(0);
                                } else {
                                    last_press_time = now;
                                    printf("STEP:1:2\n");
                                    fflush(stdout);
                                }
                            }
                        } else if (mode == MODE_TRIPLE_VOL_DOWN) {
                            if (code == KEY_VOLUMEDOWN) {
                                if (now - last_press_time < 1200) {
                                    press_count++;
                                    last_press_time = now;
                                    if (press_count >= 2) {
                                        printf("UNLOCKED:triple_volume_down\n");
                                        fflush(stdout);
                                        handle_signal(0);
                                    } else {
                                        printf("STEP:2:3\n");
                                        fflush(stdout);
                                    }
                                } else {
                                    press_count = 1;
                                    last_press_time = now;
                                    printf("STEP:1:3\n");
                                    fflush(stdout);
                                }
                            }
                        } else if (mode == MODE_DOUBLE_VOL_UP) {
                            if (code == KEY_VOLUMEUP) {
                                if (now - last_press_time < 1000) {
                                    printf("UNLOCKED:double_volume_up\n");
                                    fflush(stdout);
                                    handle_signal(0);
                                } else {
                                    last_press_time = now;
                                    printf("STEP:1:2\n");
                                    fflush(stdout);
                                }
                            }
                        } else if (mode == MODE_VOL_UP_THEN_DOWN) {
                            if (code == KEY_VOLUMEUP) {
                                last_key_code = KEY_VOLUMEUP;
                                last_press_time = now;
                                printf("STEP:1:2\n");
                                fflush(stdout);
                            } else if (code == KEY_VOLUMEDOWN) {
                                if (last_key_code == KEY_VOLUMEUP && now - last_press_time < 1500) {
                                    printf("UNLOCKED:volume_up_then_down\n");
                                    fflush(stdout);
                                    handle_signal(0);
                                } else {
                                    last_key_code = 0;
                                    printf("STEP:WRONG\n");
                                    fflush(stdout);
                                }
                            }
                        } else if (mode == MODE_VOL_DOWN_THEN_UP) {
                            if (code == KEY_VOLUMEDOWN) {
                                last_key_code = KEY_VOLUMEDOWN;
                                last_press_time = now;
                                printf("STEP:1:2\n");
                                fflush(stdout);
                            } else if (code == KEY_VOLUMEUP) {
                                if (last_key_code == KEY_VOLUMEDOWN && now - last_press_time < 1500) {
                                    printf("UNLOCKED:volume_down_then_up\n");
                                    fflush(stdout);
                                    handle_signal(0);
                                } else {
                                    last_key_code = 0;
                                    printf("STEP:WRONG\n");
                                    fflush(stdout);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    handle_signal(0);
    return 0;
}
