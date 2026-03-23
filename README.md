# ☠️ DeathNote Plugin

A Bukkit/Paper plugin for Minecraft 1.21 that recreates the Death Note anime experience as a multiplayer game with roles, missions, and special abilities.

---

## 📋 Requirements

- **Java:** 21+
- **Server:** Paper 1.21 (or compatible fork)
- **Dependencies:** None (all included in the JAR)

---

## 📦 Installation

1. Build the JAR using IntelliJ IDEA (`Build > Build Artifacts > deahtNote:jar`).
2. Copy the `deahtNote.jar` to your server's `plugins/` folder.
3. Start the server.
4. The following files will be generated automatically:
   - `plugins/deahtNote/config.yml` — Main configuration
   - `plugins/deahtNote/zones.yml` — Mission zones
   - `plugins/deahtNote/lang/es.yml` — Spanish messages
   - `plugins/deahtNote/lang/en.yml` — English messages
   - `plugins/deahtNote/lang/fr.yml` — French messages
   - `plugins/deahtNote/data.db` — SQLite player database

---

## 🌍 Language System

The plugin supports **3 languages**: Spanish (`es`), English (`en`), and French (`fr`).

To change the language, edit `config.yml`:

```yaml
language: es   # Options: es, en, fr
```

After changing, restart the server or use `/reload`.  
All plugin messages will update to the new language automatically.

To customize messages, edit the corresponding `lang/xx.yml` file directly.

---

## 🎭 Roles

| Role | Description |
|------|-------------|
| **Kira** | Can write names in the Death Note to kill players after a delay. Has the Shinigami Deal and role-swap abilities. |
| **L** | World's greatest detective. Assigns investigation missions to other players. |
| **Mikami** | Kira's follower. Can steal identifications from other players faster. |
| **Mello** | L's rival. Linked with Near — if one dies, the other follows. Has combat bonuses. |
| **Near** | L's successor. Becomes the new L when L dies. Linked with Mello. |
| **Watari** | L's support. Can transfer identifications to L and reveal player roles. |
| **Investigador** | Default role. Wins by surviving and contributing to Kira's capture. |

---

## ⚡ Special Systems

### 🗒️ Death Note
Kira receives a **WRITTEN_BOOK** item. Right-clicking opens a GUI showing all online players. Kira can click a player's head to write their name — that player will die after `kill_timing` seconds (configurable). Kira must have the player's **identification paper** to be able to kill them.

### 👁️ Shinigami Deal
Kira can activate the **Shinigami Deal** by right-clicking an activation item. This permanently halves Kira's health but grants:
- Faster steal cooldown
- Ability to see other players' health in the Action Bar

### 🔄 Role Swap (Kira ↔ Mikami)
Kira receives a **role swap item**. Right-clicking it swaps Kira's role with Mikami's. The item is consumed after use. A cooldown applies.

### 📋 Missions (L System)
L receives a **mission item** (compass). Right-clicking opens a GUI where L can:
1. Select a mission zone
2. Select players to assign
3. Confirm the mission

Players assigned must travel to the zone. A BossBar shows real-time progress. If both players are in the zone, progress is normal. If only one is present, progress is slow. A failure penalty removes 1 heart from L.

### 🃏 Identifications
All players start with an **identification paper** (paper item with their name). These papers are required for Kira to kill that player. Players can steal each other's IDs. Watari can transfer IDs to L along with the real role information.

---

## 💾 Database

Player data is persisted to a **SQLite database** (`data.db`):
- Player roles
- Alive/dead status
- Identifications owned
- Shinigami deal status
- Steal cooldowns

Data loads on server start and saves on server stop.

---

## ⌨️ Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/distributeroles` | `deathnote.admin` | Randomly distributes roles to all online players |
| `/debug` | `deathnote.admin` | Shows debug information for the sender |
| `/missiontp <zone>` | `deathnote.admin` | Teleports the sender to a mission zone |
| `/s <message>` | `deathnote.player` | Sends an anonymous message to all |
| `/m <player> <message>` | `deathnote.player` | Sends an anonymous whisper to a player |
| `/missiondebug` | `deathnote.admin` | Shows mission GUI debug state |
| `/missionclear` | `deathnote.admin` | Force-clears mission GUI state for sender |

---

## ⚙️ Configuration (`config.yml`)

```yaml
# Language: es, en, fr
language: es

play:
  min-players: 4
  max-players: 12
  permision: deathnote.staff

roles:
  kira:
    kill_timing: 40        # Seconds before Death Note kills
    kill_cooldown: 60      # Seconds between kills

shinigami_eye_deal:
  max_health_reduction: 10.0
  steal_cooldown_reduction: 0.5

stealing:
  prevent_duplicate_ids: true
  victim_loses_id: true

identification:
  clear_on_death: true     # Remove plugin items on death, NOT regular items

death_messages:
  show_role: true
  broadcast_to_all: true
  format: "&c{player} ha muerto. Era {role}"

role_swap_system:
  cooldown: 300            # Seconds
  swap_item:
    material: NETHER_STAR
    name: "&6Intercambio de Roles"

mission_system:
  mission:
    max_players: 2
    reward_exp: 50
    max_duration: 300
  mission_item:
    material: COMPASS
    name: "&bMisión de L"
    cooldown: 60

death_chain:
  mello_near_link: true    # Near/Mello die together

role_distribution:
  priority_roles:
    - kira
    - l
    - mikami
    - mello
    - near
    - watari
  default_role: Investigador
```

---

## 🗺️ Zones Configuration (`zones.yml`)

```yaml
zones:
  plaza_central:
    name: "Plaza Central"
    world: world
    center_x: 0
    center_z: 0
    radius: 50
    duration: 120
    icon: STONE
    pause_enabled: true
    teleport_location:
      x: 0.5
      y: 64.0
      z: 0.5
      yaw: 0.0
      pitch: 0.0
```

---

## 🏆 Victory Conditions

| Winner | Condition |
|--------|-----------|
| **Kira** | Both L and Near are eliminated |
| **L** | Kira is killed |
| **Near** | Inherited from L succession and outlasts Kira |
| **Mello** | Kills both Kira and L |
| **Investigadores** | Kira is killed by an investigator |

---

## 🔧 Development

- **Language:** Java 21
- **Build tool:** Maven
- **API:** Paper 1.21 (`io.papermc:papermc-api:1.21-R0.1-SNAPSHOT`)
- **Database:** SQLite (embedded via `org.xerial:sqlite-jdbc`)
- **Utilities:** Lombok, Adventure API

Build with:
```
Build > Build Artifacts > deahtNote:jar (IntelliJ IDEA)
```

---

*Plugin developed for a private Death Note-themed Minecraft server. Roles and mechanics inspired by the Death Note anime/manga.*
