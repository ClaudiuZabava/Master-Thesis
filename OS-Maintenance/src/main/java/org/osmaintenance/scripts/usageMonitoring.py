import time
import sqlite3
import ctypes
import win32gui
import win32process
import psutil
from datetime import datetime
import signal

class LASTINPUTINFO(ctypes.Structure):
    _fields_ = [("cbSize", ctypes.c_uint), ("dwTime", ctypes.c_uint)]

class AppUsageMonitor:
    def __init__(self):
        self.conn = None
        self.c = None
        self.running = True
        self.current_app = ''
        self.start_time = datetime.now()
        self.real_active_time = 0
        signal.signal(signal.SIGTERM, self.handle_sigterm)

    def handle_sigterm(self, signum, frame):
        print("📥 SIGTERM received.")
        self.running = False  # nu oprim aici, lăsăm bucla să continue

    def get_active_window(self):
        return win32gui.GetForegroundWindow()

    def get_app_name(self, window_handle):
        try:
            _, pid = win32process.GetWindowThreadProcessId(window_handle)
            return psutil.Process(pid).name()
        except:
            return "unknown"

    def get_idle_duration(self):
        lastInputInfo = LASTINPUTINFO()
        lastInputInfo.cbSize = ctypes.sizeof(lastInputInfo)
        if ctypes.windll.user32.GetLastInputInfo(ctypes.byref(lastInputInfo)):
            millis = ctypes.windll.kernel32.GetTickCount() - lastInputInfo.dwTime
            return millis / 1000.0
        return 0

    def start_monitoring(self):
        self.conn = sqlite3.connect('app_usage.db')
        self.c = self.conn.cursor()
        self.c.execute("""
            CREATE TABLE IF NOT EXISTS usagestat (
                appname TEXT,
                currentdate TEXT,
                starttime TEXT,
                endtime TEXT,
                usagetime INTEGER,
                realactivetime INTEGER
            )
        """)

        idle_time_start = None

        print("Monitoring started.")

        try:
            while self.running:
                try:
                    new_app_handle = self.get_active_window()
                    new_app_name = self.get_app_name(new_app_handle)
                    idle_duration = self.get_idle_duration()

                    if idle_duration > 10.0:
                        if idle_time_start is None:
                            idle_time_start = datetime.now()
                    else:
                        if idle_time_start is not None:
                            idle_gap = (datetime.now() - idle_time_start).seconds
                            self.real_active_time -= idle_gap
                            idle_time_start = None

                    if new_app_name != self.current_app:
                        self.save_current_session()

                        self.current_app = new_app_name
                        self.real_active_time = 0
                        self.start_time = datetime.now()

                    time.sleep(1)
                except Exception as e:
                    print(f"[Eroare în buclă]: {e}")
        finally:
            self.save_current_session()
            self.conn.commit()
            self.conn.close()
            print("Monitorizarea s-a încheiat.")

    def save_current_session(self):
        if self.current_app and self.current_app != '':
            end_time = datetime.now()
            usage_time = (end_time - self.start_time).seconds
            real_time = usage_time - abs(self.real_active_time)

            usage_minutes = usage_time_seconds // 60
            real_minutes = real_time_seconds // 60

            self.c.execute("""
                INSERT INTO usagestat VALUES (?, ?, ?, ?, ?, ?)
            """, (
                current_app,
                str(datetime.now().date()),
                str(self.start_time),
                str(end_time),
                usage_time,
                real_time
            ))
            self.conn.commit()
            print(f"Sesiune salvată: {self.current_app} | {usage_time}s")

if __name__ == "__main__":
    monitor = AppUsageMonitor()
    monitor.start_monitoring()
