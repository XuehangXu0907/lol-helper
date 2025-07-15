#!/usr/bin/env python3
import requests
import json

def get_official_champions():
    """获取官方最新的英雄数据"""
    try:
        # 获取最新版本
        versions_url = "https://ddragon.leagueoflegends.com/api/versions.json"
        versions_response = requests.get(versions_url)
        latest_version = versions_response.json()[0]
        print(f"Latest version: {latest_version}")
        
        # 获取英雄数据
        champions_url = f"https://ddragon.leagueoflegends.com/cdn/{latest_version}/data/en_US/champion.json"
        response = requests.get(champions_url)
        data = response.json()
        
        official_champions = {}
        for key, champion in data['data'].items():
            official_champions[key] = {
                'id': champion['key'],
                'name': champion['name'],
                'title': champion['title']
            }
        
        return official_champions, latest_version
    except Exception as e:
        print(f"Error fetching official data: {e}")
        return {}, "unknown"

def extract_local_champions():
    """从本地文件提取英雄key列表"""
    local_champions = [
        "Aatrox", "Ahri", "Akali", "Akshan", "Alistar", "Amumu", "Anivia", "Annie",
        "Aphelios", "Ashe", "AurelionSol", "Aurora", "Azir", "Bard", "Belveth", "Blitzcrank",
        "Brand", "Braum", "Briar", "Caitlyn", "Camille", "Cassiopeia", "Chogath", "Corki",
        "Darius", "Diana", "DrMundo", "Draven", "Ekko", "Elise", "Evelynn", "Ezreal",
        "Fiddlesticks", "Fiora", "Fizz", "Galio", "Gangplank", "Garen", "Gnar", "Gragas",
        "Graves", "Gwen", "Hecarim", "Heimerdinger", "Hwei", "Illaoi", "Irelia", "Ivern",
        "Janna", "JarvanIV", "Jax", "Jayce", "Jhin", "Jinx", "Kaisa", "Kalista",
        "Karma", "Karthus", "Kassadin", "Katarina", "Kayle", "Kayn", "Kennen", "Khazix",
        "Kindred", "Kled", "KogMaw", "Leblanc", "LeeSin", "Leona", "Lillia", "Lissandra",
        "Lucian", "Lulu", "Lux", "Malphite", "Malzahar", "Maokai", "MasterYi", "Milio",
        "MissFortune", "Mordekaiser", "Morgana", "Nami", "Nasus", "Nautilus", "Neeko", "Nidalee",
        "Nilah", "Nocturne", "Nunu", "Olaf", "Orianna", "Ornn", "Pantheon", "Poppy",
        "Pyke", "Qiyana", "Quinn", "Rakan", "Rammus", "RekSai", "Rell", "Renata",
        "Renekton", "Rengar", "Riven", "Rumble", "Ryze", "Samira", "Sejuani", "Senna",
        "Seraphine", "Sett", "Shaco", "Shen", "Shyvana", "Singed", "Sion", "Sivir",
        "Skarner", "Smolder", "Sona", "Soraka", "Swain", "Sylas", "Syndra", "TahmKench",
        "Taliyah", "Talon", "Taric", "Teemo", "Thresh", "Tristana", "Trundle", "Tryndamere",
        "TwistedFate", "Twitch", "Udyr", "Urgot", "Varus", "Vayne", "Veigar", "Velkoz",
        "Vex", "Vi", "Viego", "Viktor", "Vladimir", "Volibear", "Warwick", "MonkeyKing",
        "Xayah", "Xerath", "XinZhao", "Yasuo", "Yone", "Yorick", "Yuumi", "Zac",
        "Zed", "Zeri", "Ziggs", "Zilean", "Zoe", "Zyra"
    ]
    return set(local_champions)

def compare_champions():
    """对比本地和官方的英雄数据"""
    print("Getting official champion data...")
    official_champions, version = get_official_champions()
    
    print("Extracting local champion data...")
    local_champions = extract_local_champions()
    
    official_keys = set(official_champions.keys())
    
    print(f"\n=== Champion Data Comparison Report (Official Version: {version}) ===")
    print(f"Local champions count: {len(local_champions)}")
    print(f"Official champions count: {len(official_keys)}")
    
    # Find champions missing in local
    missing_in_local = official_keys - local_champions
    if missing_in_local:
        print(f"\nChampions missing in local ({len(missing_in_local)}):")
        for key in sorted(missing_in_local):
            champion = official_champions[key]
            print(f"  - {key} (ID: {champion['id']}, Name: {champion['name']}, Title: {champion['title']})")
    
    # Find extra champions in local (possibly incorrect key names)
    extra_in_local = local_champions - official_keys
    if extra_in_local:
        print(f"\nExtra champion keys in local ({len(extra_in_local)}):")
        for key in sorted(extra_in_local):
            print(f"  - {key}")
            # Try to find similar official keys
            similar_keys = [ok for ok in official_keys if ok.lower() == key.lower() or ok.replace("'", "").replace(" ", "") == key.replace("'", "").replace(" ", "")]
            if similar_keys:
                print(f"    Possible match: {similar_keys[0]}")
    
    # Display complete list of official champion keys
    print(f"\n=== Complete Official Champion Key List ({len(official_keys)}) ===")
    for key in sorted(official_keys):
        champion = official_champions[key]
        print(f"{key}: {champion['name']} - {champion['title']}")

if __name__ == "__main__":
    compare_champions()