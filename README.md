# Terrarium
**Pixel-Perfect Earth Terrain for Minecraft**  

Generate pixel-perfect worlds from real elevation data, with dynamic tile loading and customizable terrain shaping.  
- World type is `terrarium:terrarium` for servers, singleplayer, select earth when creating world and choose customize to change settings per world.

---

## 🌐 Terrain Data Pipeline
- **Source**: AWS S3 (`elevation-tiles-prod/terrarium/{zoom}/{x}/{y}.png`)
- **Temperature and Precipitation** Personal Source Hosted on GitHub, I compiled this using QGIS. (`clim-monthly/{month}/{zoom}/{x}/{y}`) **This is a temporary hosting solution**
- **Pixel-to-Block**: 1 pixel = 1 Minecraft block.  
- **Tile System**:  
  - Each tile = `256×256` pixels (`256×256` blocks ingame).  
  - `zoom` level determines tile count per axis (`2^zoom` tiles).  
    - Example: `zoom: 8` = `2⁸ = 256` tiles → `256×256 = 65,536` blocks wide.  

---

## 🗃️ Downloads
- You can download indev test versions [here](https://nightly.link/ly-nxs/terrarium/workflows/build/1.21-World/Artifacts)
- Support is not guarunteed for those, only alphas, betas, and releases
- FabricAPI is required
---

## 🏔️ Technical Notes
- **Scale Examples**:
  - zoom: 10 = 1,024 tiles → 262,144×262,144 blocks.
  - zoom: 13 = 8,192 tiles → 2,097,152×2,097,152 blocks.

### 🌩️ Performance:
- Higher zoom = larger worlds but slower generation.
- Reduce zoom to 8–10 for survival-friendly sizes.
### World Size Comparison
- zoom: 10 (small - ~1:48) vs. zoom: 13 (planetary-scale - ~1:6).
### 🖼️ Screenshots:

- Taken with v0.0.2-beta.2 + Bliss/Photon + WWOO

**Zoom: 13 | Height 768**

---
![2025-04-20_17 58 57](https://github.com/user-attachments/assets/77465cca-cd72-4c7f-95ac-47b6e18b9804)
![2025-04-20_11 15 43](https://github.com/user-attachments/assets/7e331e0b-4763-4a60-8a79-dacce193363e)
![2025-04-20_11 15 26](https://github.com/user-attachments/assets/003d848f-57a5-4cd0-ae9e-36180ea089a0)


---
## 🛠️ How It Works
- Tile Fetching: Downloads 256×256 PNG tiles from AWS based on zoom and caches.
- Height Mapping: Converts RGB pixels to block heights, scaled by additionalAlt.
- Biome Placement: Vanilla biomes mapped using elevation (startingY + altitudeDropoff).

## 🔧 **Configuration**  
![image](https://github.com/user-attachments/assets/14d2257e-59a9-4518-846e-d3220470d991)

`/config/BlossomMods/Terrarium.json`:  
```json
{   
  "ELEVATION_URL": "https://s3.amazonaws.com/elevation-tiles-prod/terrarium/", //elevation data source
  "TEMPERATURE_URL": "https://raw.githubusercontent.com/ly-nxs/terrarium-data/refs/heads/main/tiles/climate-monthly/", //climate data source
  "CACHE_DIR": "./tiles", //tile cache dir
}
