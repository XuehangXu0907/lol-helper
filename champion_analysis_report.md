# 英雄数据分析报告

## 从 ChampionDataManager.java 提取的英雄 Key 列表

以下是从 `D:\lol-helper\src\main\java\com\lol\championselector\manager\ChampionDataManager.java` 文件中提取的所有英雄 key 名称（共166个）：

### A系列
- Aatrox, Ahri, Akali, Akshan, Alistar, Amumu, Anivia, Annie, Aphelios, Ashe, AurelionSol, Aurora, Azir

### B系列
- Bard, Belveth, Blitzcrank, Brand, Braum, Briar

### C系列
- Caitlyn, Camille, Cassiopeia, Chogath, Corki

### D系列
- Darius, Diana, DrMundo, Draven

### E系列
- Ekko, Elise, Evelynn, Ezreal

### F系列
- Fiddlesticks, Fiora, Fizz

### G系列
- Galio, Gangplank, Garen, Gnar, Gragas, Graves, Gwen

### H系列
- Hecarim, Heimerdinger, Hwei

### I系列
- Illaoi, Irelia, Ivern

### J系列
- Janna, JarvanIV, Jax, Jayce, Jhin, Jinx

### K系列
- Kaisa, Kalista, Karma, Karthus, Kassadin, Katarina, Kayle, Kayn, Kennen, Khazix, Kindred, Kled, KogMaw

### L系列
- Leblanc, LeeSin, Leona, Lillia, Lissandra, Lucian, Lulu, Lux

### M系列
- Malphite, Malzahar, Maokai, MasterYi, Milio, MissFortune, Mordekaiser, Morgana

### N系列
- Nami, Nasus, Nautilus, Neeko, Nidalee, Nilah, Nocturne, Nunu

### O系列
- Olaf, Orianna, Ornn

### P系列
- Pantheon, Poppy, Pyke

### Q系列
- Qiyana, Quinn

### R系列
- Rakan, Rammus, RekSai, Rell, Renata, Renekton, Rengar, Riven, Rumble, Ryze

### S系列
- Samira, Sejuani, Senna, Seraphine, Sett, Shaco, Shen, Shyvana, Singed, Sion, Sivir, Skarner, Smolder, Sona, Soraka, Swain, Sylas, Syndra

### T系列
- TahmKench, Taliyah, Talon, Taric, Teemo, Thresh, Tristana, Trundle, Tryndamere, TwistedFate, Twitch

### U系列
- Udyr, Urgot

### V系列
- Varus, Vayne, Veigar, Velkoz, Vex, Vi, Viego, Viktor, Vladimir, Volibear

### W系列
- Warwick, MonkeyKing (Wukong)

### X系列
- Xayah, Xerath, XinZhao

### Y系列
- Yasuo, Yone, Yorick, Yuumi

### Z系列
- Zac, Zed, Zeri, Ziggs, Zilean, Zoe, Zyra

## 与官方最新数据对比（版本：15.14.1）

### 统计信息
- **本地英雄数量**: 166
- **官方英雄数量**: 171
- **缺失英雄数量**: 5

### 本地数据缺失的英雄
以下5个英雄在官方数据中存在，但本地数据中缺失：

1. **Ambessa** (ID: 799)
   - 名称: Ambessa
   - 称号: Matriarch of War

2. **KSante** (ID: 897)
   - 名称: K'Sante
   - 称号: the Pride of Nazumah

3. **Mel** (ID: 800)
   - 名称: Mel
   - 称号: the Soul's Reflection

4. **Naafiri** (ID: 950)
   - 名称: Naafiri
   - 称号: the Hound of a Hundred Bites

5. **Yunara** (ID: 804)
   - 名称: Yunara
   - 称号: the Unbroken Faith

### 建议更新
建议在 `ChampionDataManager.java` 文件中添加这5个缺失的英雄数据，以保持与官方最新数据的同步。

### 数据完整性
本地数据覆盖率：**97.1%** (166/171)

除了缺失的5个新英雄外，所有英雄的 key 名称都与官方数据完全匹配，数据质量很高。