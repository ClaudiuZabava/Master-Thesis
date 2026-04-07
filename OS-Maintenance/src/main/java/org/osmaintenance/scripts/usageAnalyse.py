import sqlite3
import pandas as pd
import matplotlib.pyplot as plt
from datetime import datetime
import os

def get_total_pc_usage_last_7_recorded_days(db_path='app_usage.db'):
    # 1. Citim datele brute
    conn = sqlite3.connect(db_path)
    query = """
        SELECT currentdate, usagetime
        FROM usagestat
    """
    df = pd.read_sql_query(query, conn)
    conn.close()

    # 2. Conversie dată + curățare
    df['currentdate'] = pd.to_datetime(df['currentdate'], errors='coerce')
    df.dropna(inplace=True)

    # 3. Grupăm per zi și adunăm timpul
    grouped = df.groupby(df['currentdate'].dt.date)['usagetime'].sum().reset_index()

    # 4. Sortăm zilele descrescător și luăm ultimele 7 zile înregistrate
    last_7_days = grouped.sort_values(by='currentdate', ascending=False).head(7)

    # 5. Reordonăm cronologic pentru grafic
    last_7_days = last_7_days.sort_values(by='currentdate')

    # 6. Transformăm în dict {data_str: secunde}
    usage_dict = {
        str(row['currentdate']): row['usagetime']
        for _, row in last_7_days.iterrows()
    }

    return usage_dict

def generate_pc_time_chart(usage_dict, output_path='statistics/pc_time.png'):
    dates = list(usage_dict.keys())
    times_hours = [round(seconds / 3600, 2) for seconds in usage_dict.values()]

    os.makedirs(os.path.dirname(output_path), exist_ok=True)

    plt.figure(figsize=(6, 4), dpi=100)
    ax = plt.gca()
    ax.set_facecolor("#2c2d42")

    ax.plot(dates, times_hours, color='#5176e8', marker='o', linewidth=2)

    ax.set_title("PC screen time", color='white')
    ax.set_xlabel("", color='white')
    ax.set_ylabel("", color='white')
    plt.xticks(rotation=15, color='white')
    plt.yticks(color='white')

    for spine in ax.spines.values():
        spine.set_color('white')

    plt.tight_layout()
    plt.savefig(output_path, facecolor='#2c2d42')
    plt.close()

if __name__ == "__main__":
    usage = get_total_pc_usage_last_7_recorded_days()
    generate_pc_time_chart(usage)
    print("Grafic salvat ca: statistics/pc_time.png")
