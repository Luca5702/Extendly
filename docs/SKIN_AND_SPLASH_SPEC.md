# 「松息 Sōngxī」五维皮肤系统与「开屏雅鉴」工程技术规范（`SKIN_AND_SPLASH_SPEC.md`）

本规范是「松息（Sōngxī）」跨模块共享的单一事实来源（Single Source of Truth），供原生 Android 客户端（`:core:designsystem`、`:feature:splashad`、`:feature:stretch`、`:feature:sanctuary`）与高保真交互套件（`songxi_skin_gallery_showcase.html`、`songxi_interactive_prototype.html`）严格对齐实现。全应用遵循**零内购门槛、无等级锁限，全功能永久免费开放（100% 永久免费 · 100% 免费，无任何内购分档），应用内全套功法、全部四款山水插画画境与四册雅集全部免费开放；遵循「只以开屏广告盈利」准则，只以单页合规克制的 3 秒「开屏雅鉴」（开屏广告）作为唯一商业支持入口**的纯净商业准则。

---

## 1. 五维可替换皮肤系统契约（5D `SongxiSkin` Specification）

### 1.1 Kotlin 领域令牌结构（`com.songxi.app.core.designsystem`）

每一款内置皮肤均由不可变数据聚合体 `SongxiSkin` 完整定义，经由 `LocalSongxiSkin` 与 `SongxiTheme` 注入全应用组件树（Web 原型端严格限定于 `.songxi-app-shell[data-skin="<skinId>"]` 与 `.songxi-showcase-card[data-skin="<skinId>"]` 局部容器作用域，绝不声明宿主全局样式覆盖）：

```kotlin
package com.songxi.app.core.designsystem

import androidx.compose.ui.graphics.Color
import com.songxi.app.core.model.SkinId
import com.songxi.app.core.model.SoundProfileId

enum class RidgeStyle {
    INK_WASH_MIST,            // 米氏云山泼墨晕染
    QINGLV_MINERAL_GOLD_RIM,  // 千里江山石青石绿泥金勾峰
    DUSK_INDIGO_LAYER,        // 暮山苍靛远岫层峦
    SPRING_SLOPE_STREAM       // 春涧缓坡蜿蜒碧溪
}

enum class CelestialType { HAZY_SUN, GOLDEN_MIST_ORB, CRESCENT_MOON, WARM_MORNING_GLOW }
enum class BotanicalMotif { PINE_BRANCH, BAMBOO_AND_ROCK, NIGHT_PINE_NEEDLES, PEACH_BLOSSOM_TWIG }
enum class FigureStyle { INK_BRUSH_WASH, EDITORIAL_MINERAL_FLAT, MOONLIT_CONTOUR, BOTANICAL_GOUACHE }
enum class HaloGeometry { INK_CONCENTRIC_RIPPLE, JADE_CLOUD_AURA, LUNAR_TIDE_RING, PETAL_BLOOM_HALO }
enum class SealShape { SQUARE_ZHUWEN, OBLONG_BAIWEN, GOURD_XIANZHANG, ROUND_ZHUWEN }

data class SkinPaletteTokens(
    val isDark: Boolean,
    val canvasPrimary: Color,
    val canvasSkyWashTop: Color,
    val surfaceCard: Color,
    val surfaceCardSubtle: Color,
    val surfaceOverlay: Color,
    val borderHairline: Color,
    val inkPrimary: Color,
    val inkSecondary: Color,
    val inkTertiary: Color,
    val accentPrimary: Color,
    val accentPrimarySoft: Color,
    val accentPrimaryGlow: Color,
    val accentSecondary: Color,
    val accentSecondarySoft: Color,
    val sealVermilion: Color,
    val onAccent: Color,
    val substrateGrainAlpha: Float,
    val cardCornerRadiusDp: Int
)

data class SkinLandscapeTokens(
    val ridgeStyle: RidgeStyle,
    val farRidgeColor: Color,
    val midRidgeColor: Color,
    val ridgeCrestStrokeColor: Color?,
    val mistColor: Color,
    val mistOpacity: Float,
    val celestialType: CelestialType,
    val celestialColor: Color,
    val botanicalMotif: BotanicalMotif
)

data class SkinIllustrationTokens(
    val figureStyle: FigureStyle,
    val garmentFill: Color,
    val garmentShadow: Color,
    val skinToneFill: Color,
    val hairFill: Color,
    val contourStroke: Color,
    val contourStrokeWidthDp: Float,
    val haloGeometry: HaloGeometry,
    val sealShape: SealShape,
    val sealText: String
)

data class SkinEditorialTokens(
    val titleCn: String,
    val subtitleCn: String,
    val fusionBadge: String,
    val greetingPrefix: String,
    val completionVerse: String,
    val titleLetterSpacingSp: Float
)

data class SkinSensoryTokens(
    val defaultSoundProfile: SoundProfileId,
    val baseFrequencyHz: Float,
    val harmonicRatios: List<Float>,
    val inhaleDurationMs: Int = 4000,
    val exhaleDurationMs: Int = 6000,
    val hapticAmplitude: Int
)

data class SongxiSkin(
    val id: SkinId,
    val meta: SkinEditorialTokens,
    val palette: SkinPaletteTokens,
    val landscape: SkinLandscapeTokens,
    val illustration: SkinIllustrationTokens,
    val sensory: SkinSensoryTokens
)
```

---

### 1.2 维度 A —— 四款免费皮肤矿物与宣绢色板及 WCAG 2.1 对比度校准表

全系严格遵循宋画 **60% 宣绢基底 / 25% 云水晕染 / 10% 骨法墨色 / 5% 矿物与朱砂印泥** 设色法则，彻底禁用刺眼纯白极值与死寂纯黑极值：

| 令牌属性 (`SkinPaletteTokens`) | `INK_MIST` (`ink_mist`)<br/>**水墨远山 · 素宣** | `QINGLV_CELADON` (`qinglv_celadon`)<br/>**青绿山居 · 汝瓷** | `DUSK_PINE` (`dusk_pine`)<br/>**暮山听松 · 沉香** | `SPRING_BLOSSOM` (`spring_blossom`)<br/>**春涧花影 · 暖陶** |
| :--- | :--- | :--- | :--- | :--- |
| `isDark` | `false` | `false` | `true` | `false` |
| `canvasPrimary`（60% 主画布） | `#F5F1E8`（手工生宣） | `#EEF3EE`（天青熟绢） | `#19201E`（苍墨夜山） | `#F8F1EA`（暖桑皮纸） |
| `canvasSkyWashTop` | `#EBE5D8` | `#DFECE6` | `#212C29` | `#F2E5DA` |
| `surfaceCard` | `#FAF7F1` | `#F7FAF7` | `#232D2A` | `#FDF9F5` |
| `surfaceCardSubtle`（25% 次底） | `#ECE6DA` | `#E3EDE7` | `#1D2623` | `#EFE4DA` |
| `surfaceOverlay` | `#F5F1E8` (`alpha 0.94`) | `#EEF3EE` (`alpha 0.94`) | `#161C1A` (`alpha 0.95`) | `#F8F1EA` (`alpha 0.94`) |
| `borderHairline` | `#DCD5C7` | `#CBDAD2` | `#33413D` | `#E2D3C6` |
| `inkPrimary`（10% 骨法主墨） | `#262B28`（松烟墨） | `#1E2E2B`（黛青墨） | `#E6E1D6`（素绢暖白） | `#322926`（紫砂茶墨） |
| `inkSecondary` | `#5C645F` | `#4F635E` | `#A6B2AB` | `#685A54` |
| `inkTertiary`（辅助说明墨色） | `#5E6661` | `#536762` | `#98A59E` | `#6E5F58` |
| `accentPrimary`（5% 主矿物色） | `#486356`（苍葭青） | `#35685C`（石绿青瓷） | `#86AA96`（竹月清辉） | `#9B5A49`（暖陶赭粉） |
| `accentPrimarySoft` | `#DCE5DF` | `#D6E7E1` | `#2B3B35` | `#FAF0EC` |
| `accentPrimaryGlow` | `#84A192` | `#72A89B` | `#587D6A` | `#CE9281` |
| `accentSecondary` | `#91674B`（赭石茶） | `#2F5669`（石青） | `#C79A7A`（沉香琥珀） | `#4E7266`（春涧碧） |
| `accentSecondarySoft` | `#EDE2D8` | `#DAE6EC` | `#38302A` | `#DCE8E3` |
| `sealVermilion`（朱砂印泥） | `#B5493A` | `#B84435` | `#D47566` | `#B54639` |
| `onAccent`（主按钮文字） | `#FAF7F1` | `#F7FAF7` | `#161D1B` | `#FDF9F5` |
| `substrateGrainAlpha` | `0.035f` | `0.028f` | `0.040f` | `0.032f` |
| `cardCornerRadiusDp` | `24` | `20` | `24` | `26` |

#### 数学验证通过的 WCAG 2.1 相对亮度对比度表

按公式 $(L_{\max} + 0.05) / (L_{\min} + 0.05)$ 精确核验，四款皮肤全部关键前景与背景组合均满足 `>= 4.5:1`（主墨色全部达 AAA 级 `>= 10.88:1`）：

| 对比度检测组合 | `ink_mist` | `qinglv_celadon` | `dusk_pine` | `spring_blossom` | WCAG 等级 |
| :--- | ---: | ---: | ---: | ---: | :--- |
| `inkPrimary` vs `canvasPrimary` | **12.77 : 1** | **12.62 : 1** | **12.71 : 1** | **12.67 : 1** | **AAA** (`>= 7.0:1`) |
| `inkPrimary` vs `surfaceCard` | **13.46 : 1** | **13.49 : 1** | **10.88 : 1** | **13.54 : 1** | **AAA** (`>= 7.0:1`) |
| `inkPrimary` vs `surfaceCardSubtle` | **11.58 : 1** | **11.84 : 1** | **11.90 : 1** | **11.33 : 1** | **AAA** (`>= 7.0:1`) |
| `inkSecondary` vs `canvasPrimary` | **5.41 : 1** | **5.70 : 1** | **7.56 : 1** | **5.90 : 1** | **AA** (`>= 4.5:1`) |
| `inkSecondary` vs `surfaceCard` | **5.70 : 1** | **6.09 : 1** | **6.47 : 1** | **6.31 : 1** | **AA** (`>= 4.5:1`) |
| `inkTertiary` vs `canvasPrimary` | **5.25 : 1** | **5.36 : 1** | **6.48 : 1** | **5.45 : 1** | **AA** (`>= 4.5:1`) |
| `accentPrimary` vs `canvasPrimary` | **5.83 : 1** | **5.69 : 1** | **6.48 : 1** | **4.75 : 1** | **AA** (`>= 4.5:1`) |
| `accentPrimary` vs `accentPrimarySoft` | **5.11 : 1** | **4.98 : 1** | **4.61 : 1** | **4.74 : 1** | **AA** (`>= 4.5:1`) |
| `onAccent` vs `accentPrimary`（主按钮） | **6.15 : 1** | **6.08 : 1** | **6.69 : 1** | **5.07 : 1** | **AA** (`>= 4.5:1`) |
| `sealVermilion` vs `canvasPrimary` | **4.68 : 1** | **4.77 : 1** | **5.16 : 1** | **4.81 : 1** | **AA** (`>= 4.5:1`) |

---

### 1.3 维度 B、C、D、E —— 山水意境、人物插画、宋韵文案与泛音微震令牌表

| 维度与属性名 | `INK_MIST` (`ink_mist`) | `QINGLV_CELADON` (`qinglv_celadon`) | `DUSK_PINE` (`dusk_pine`) | `SPRING_BLOSSOM` (`spring_blossom`) |
| :--- | :--- | :--- | :--- | :--- |
| **B. `ridgeStyle`** | `INK_WASH_MIST` | `QINGLV_MINERAL_GOLD_RIM` | `DUSK_INDIGO_LAYER` | `SPRING_SLOPE_STREAM` |
| **B. `farRidgeColor` / `midRidgeColor`** | `#CFD6D0` / `#9FB0A6` | `#9EC0B5` / `#5F9486` | `#263531` / `#334842` | `#D7DFD8` / `#AEC4B8` |
| **B. `ridgeCrestStrokeColor`** | `null`（没骨泼墨） | `#B88655`（`0.9dp` 泥金勾峰） | `#4B655D`（`0.7dp` 月影微勾） | `#C99B84`（`0.8dp` 春赭柔线） |
| **B. `mistColor` (`mistOpacity`)** | `#F7F4EC` (`0.72f`) | `#F2F7F4` (`0.66f`) | `#293733` (`0.52f`) | `#FBF6F1` (`0.70f`) |
| **B. `celestialType` (`celestialColor`)** | `HAZY_SUN` (`#E3C89E`) | `GOLDEN_MIST_ORB` (`#D8B074`) | `CRESCENT_MOON` (`#E4CFA8`) | `WARM_MORNING_GLOW` (`#EBBFA9`) |
| **B. `botanicalMotif`** | `PINE_BRANCH`（苍松斜枝） | `BAMBOO_AND_ROCK`（翠竹湖石） | `NIGHT_PINE_NEEDLES`（暮松针影） | `PEACH_BLOSSOM_TWIG`（山桃春枝） |
| **C. `figureStyle`** | `INK_BRUSH_WASH` | `EDITORIAL_MINERAL_FLAT` | `MOONLIT_CONTOUR` | `BOTANICAL_GOUACHE` |
| **C. `garmentFill` / `garmentShadow`** | `#E6DFD1` / `#D2C8B6` | `#CFE2DC` / `#B3CEC5` | `#2E403B` / `#22302C` | `#EDD5C9` / `#DBBCAE` |
| **C. `skinToneFill` / `hairFill`** | `#EEDBCE` / `#2D322F` | `#F0DDD0` / `#22302D` | `#D9C5B4` / `#141B19` | `#F2DFD3` / `#3A2F2C` |
| **C. `contourStroke` (`widthDp`)** | `#262B28` (`2.2f`) | `#1E2E2B` (`1.5f`) | `#86AA96` (`1.8f`) | `#4A3B36` (`1.7f`) |
| **C. `haloGeometry`** | `INK_CONCENTRIC_RIPPLE` | `JADE_CLOUD_AURA` | `LUNAR_TIDE_RING` | `PETAL_BLOOM_HALO` |
| **C. `sealShape` (`sealText`)** | `SQUARE_ZHUWEN` (`松息`) | `OBLONG_BAIWEN` (`山居清息`) | `GOURD_XIANZHANG` (`听松`) | `ROUND_ZHUWEN` (`春山`) |
| **D. `titleCn` · `subtitleCn`** | `水墨远山` · `素宣写意` | `青绿山居` · `汝瓷重彩` | `暮山听松` · `沉香夜境` | `春涧花影` · `暖陶芳菲` |
| **D. `fusionBadge`** | `六分半宋人水墨 · 三分半现代插画` | `五分青绿山水 · 五分现代插画` | `五分半暮山烟岚 · 四分半微光插画` | `三分半春涧山水 · 六分半草木插画` |
| **D. `greetingPrefix`** | `远山淡影 · 宜沉肩缓息` | `青绿山居 · 宜开胸纳气` | `暮山听松 · 宜敛神松腰` | `春涧花影 · 宜舒腕温目` |
| **D. `completionVerse`** | `一息墨韵散，肩头万事轻。` | `身如青山立，清风入袖来。` | `松风吹解带，夜静息自宁。` | `眉间春水暖，指下花影轻。` |
| **D. `titleLetterSpacingSp`** | `1.6f` | `1.4f` | `1.8f` | `1.2f` |
| **E. `defaultSoundProfile`** | `SINGING_BOWL_432` (`bowl`) | `BAMBOO_CHIME_528` (`bamboo`) | `GUQIN_PINE_396` (`guqin`) | `SPRING_RAIN_440` (`rain`) |
| **E. `baseFrequencyHz` & `harmonicRatios`** | `432f` · `[1.0f, 1.50f, 2.74f]` | `528f` · `[1.0f, 2.00f, 3.16f]` | `396f` · `[1.0f, 1.50f, 2.00f, 3.00f]` | `440f` · `[1.0f, 1.68f, 2.50f]` |
| **E. `inhaleDurationMs` / `exhaleDurationMs` / `hapticAmplitude`** | `4000` / `6000` / `50` | `4000` / `6000` / `54` | `4000` / `6000` / `38` | `4000` / `6000` / `48` |

#### 昼夜随顺换境规则（`SkinPackRegistry.resolveEffectiveSkin`）
- 当 `autoNightDuskPine == true` 且本地小时 `hourOfDay >= 20 || hourOfDay < 8`（即 **`20:00–07:59`**，如 `20:01`、`06:30`）时，返回 `BuiltInSkins.DuskPine`（`dusk_pine`）。
- 在 **`08:00–19:59`**（如 `09:30`、`19:59`）或 `autoNightDuskPine == false` 时，返回用户主动选择的皮肤 `skinFor(selected)`。

---

## 2. 二十四式微拉伸动作库、情境主方与四时雅集契约（与 `:core:data` 1:1 对齐）

所有 `gentleCue` 引导语均控制在 `31–33` 字以内（**严格 `<= 36` 字符**），温润平和，支持全程闭眼跟练：

### 2.1 二十四式人体工学微拉伸目录（`SongxiStretchCatalog.allPoses`）

| # | `id` | `title` | `targetRegions` | 默认时长 | 双侧换边 | `gentleCue`（`<= 36` 字） | `inhaleKeyframe` (`tilt, chin, lDrop, rDrop, spine, lArm, rArm, wrist, eyesClosed`) | `exhaleKeyframe` (`tilt, chin, lDrop, rDrop, spine, lArm, rArm, wrist, eyesClosed`) |
| ---: | :--- | :--- | :--- | ---: | :---: | :--- | :--- | :--- |
| 1 | `neck_chin_tuck` | 收颌微点头 | `POSTERIOR_NECK` | `45s` | `false` | 视线平视，下颌水平微收半寸，随呼气让紧绷的后颈柔和变长。 | `(0f, 0.15f, 0.45f, 0.45f, 0.15f, 14f, 14f, 0f, false)` | `(0f, 0.85f, 0.85f, 0.85f, 0.25f, 12f, 12f, 0f, true)` |
| 2 | `neck_occiput_cradle` | 双手相扣颈后温托 | `POSTERIOR_NECK`, `THORACIC_CHEST` | `45s` | `false` | 掌心轻托后颈风池，下颌微扬半寸，借头部自重缓缓靠入温热掌心。 | `(0f, -0.35f, 0.50f, 0.50f, 0.45f, 118f, 118f, 0f, false)` | `(0f, -0.65f, 0.82f, 0.82f, 0.30f, 112f, 112f, 0f, true)` |
| 3 | `neck_fengchi_nod` | 左右风池微倾松解 | `POSTERIOR_NECK`, `TRAPEZIUS_SHOULDER` | `45s` | `true` | 摘下耳机松开牙关，头向一侧微倾停留，在绵长呼气里等颈侧变软。 | `(8f, 0.20f, 0.55f, 0.55f, 0.10f, 15f, 15f, 0f, true)` | `(22f, 0.35f, 0.95f, 0.75f, 0.05f, 10f, 12f, 0f, true)` |
| 4 | `neck_thumb_fengchi_press` | 指腹轻倚风池温养 | `POSTERIOR_NECK`, `EYE_BROW` | `45s` | `false` | 双肘轻支桌面，拇指落于脑后发际凹处，随长呼气卸下低头整日的重量。 | `(0f, -0.15f, 0.50f, 0.50f, 0.05f, 98f, 98f, 0f, true)` | `(0f, -0.45f, 0.85f, 0.85f, 0.15f, 94f, 94f, 0f, true)` |
| 5 | `shoulder_ear_melt` | 坐姿侧耳寻肩 | `TRAPEZIUS_SHOULDER`, `POSTERIOR_NECK` | `45s` | `true` | 左手轻搭左腿，不必用力掰头，像轻叹一口气，让肩膀慢慢沉向地面。 | `(10f, 0.10f, 0.60f, 0.55f, 0.10f, 16f, 16f, 0f, true)` | `(24f, 0.20f, 1.00f, 0.80f, 0.05f, 8f, 12f, 0f, true)` |
| 6 | `shoulder_scapula_sigh` | 肩胛微收长叹息 | `TRAPEZIUS_SHOULDER`, `THORACIC_CHEST` | `45s` | `false` | 吸气时双肩微微向后展开，呼气时顺着脊柱自然垂落，眉心也随之松开。 | `(0f, 0.10f, 0.20f, 0.20f, 0.40f, 28f, 28f, 0f, true)` | `(0f, 0.25f, 0.98f, 0.98f, 0.10f, 10f, 10f, 0f, true)` |
| 7 | `shoulder_double_inhale_sigh` | 生理性双吸长叹息 | `TRAPEZIUS_SHOULDER` | `45s` | `false` | 鼻吸两小口填满胸臆，随后极慢地长呼而出，让耸起的双肩瞬间卸力。 | `(0f, 0.0f, 0.15f, 0.15f, 0.35f, 22f, 22f, 0f, true)` | `(0f, 0.20f, 1.00f, 1.00f, 0.0f, 8f, 8f, 0f, true)` |
| 8 | `shoulder_water_circle` | 双肩温水画圆归位 | `TRAPEZIUS_SHOULDER`, `THORACIC_CHEST` | `45s` | `false` | 想象肩头浸在温水里，随吸气轻柔前升，随长呼气向后向下缓缓落定。 | `(0f, 0.05f, 0.18f, 0.18f, 0.30f, 32f, 32f, 0f, true)` | `(0f, 0.15f, 0.92f, 0.92f, 0.18f, 12f, 12f, 0f, true)` |
| 9 | `thoracic_desk_cat_cow` | 扶桌猫牛微浪 | `THORACIC_CHEST`, `LOWER_LUMBAR` | `50s` | `false` | 指尖轻扶桌沿，吸气轻展胸口，呼气将背部柔和拱起，逐节唤醒胸椎。 | `(0f, -0.25f, 0.70f, 0.70f, 0.75f, 58f, 58f, 0.15f, false)` | `(0f, 0.65f, 0.85f, 0.85f, -0.75f, 64f, 64f, 0.10f, true)` |
| 10 | `thoracic_clasp_chest_open` | 背后十指轻扣开胸 | `THORACIC_CHEST`, `TRAPEZIUS_SHOULDER` | `50s` | `false` | 双手在身后轻握，顺着吸气让锁骨向两侧微笑展开，不必刻意挺腰。 | `(0f, -0.15f, 0.78f, 0.78f, 0.82f, 42f, 42f, 0.10f, true)` | `(0f, 0.10f, 0.95f, 0.95f, 0.45f, 30f, 30f, 0f, true)` |
| 11 | `thoracic_chair_clavicle_smile` | 椅侧锁骨微笑展开 | `THORACIC_CHEST` | `45s` | `false` | 双手轻扶椅侧向后微展，让内扣一天的肩头温和归位，呼吸直达胸臆。 | `(0f, -0.10f, 0.72f, 0.72f, 0.70f, 46f, 46f, 0.15f, true)` | `(0f, 0.10f, 0.90f, 0.90f, 0.35f, 34f, 34f, 0f, true)` |
| 12 | `thoracic_seated_gentle_twist` | 坐姿扶椅温和扭转 | `THORACIC_CHEST`, `LOWER_LUMBAR` | `45s` | `true` | 一手轻扶椅背，随长呼气从胸口温和转向一侧，让肋间重获轻盈空间。 | `(4f, 0.10f, 0.55f, 0.65f, 0.35f, 35f, 55f, 0f, false)` | `(12f, 0.15f, 0.80f, 0.88f, 0.55f, 42f, 68f, 0f, true)` |
| 13 | `lumbar_pelvic_rock` | 椅沿骨盆前后微倾 | `LOWER_LUMBAR` | `45s` | `false` | 坐于椅前三分之一，吸气轻立腰椎，呼气微卷尾骨，唤醒腰骶温和流动。 | `(0f, 0.05f, 0.60f, 0.60f, 0.55f, 20f, 20f, 0f, true)` | `(0f, 0.35f, 0.85f, 0.85f, -0.55f, 18f, 18f, 0f, true)` |
| 14 | `lumbar_willow_side_bend` | 坐姿柳枝脊柱侧弯 | `LOWER_LUMBAR`, `THORACIC_CHEST` | `50s` | `true` | 一手轻撑椅面，另一臂如柳枝顺呼吸向头顶延展，舒展久坐紧缩的侧腰。 | `(8f, 0.0f, 0.45f, 0.75f, 0.30f, 138f, 24f, 0.10f, false)` | `(16f, 0.10f, 0.65f, 0.92f, 0.50f, 156f, 18f, 0.20f, true)` |
| 15 | `lumbar_seated_figure_four` | 坐姿搭膝臀腰轻舒 | `LOWER_LUMBAR` | `45s` | `true` | 一踝轻搭对侧膝上，保持背部舒展随呼气微微前倾，化开臀腰深处酸沉。 | `(0f, 0.10f, 0.60f, 0.60f, 0.25f, 26f, 26f, 0f, true)` | `(0f, 0.35f, 0.88f, 0.88f, -0.45f, 34f, 34f, 0f, true)` |
| 16 | `lumbar_bedside_butterfly_fold` | 床沿蝴蝶轻俯释腰 | `LOWER_LUMBAR` | `60s` | `false` | 脚心相对松散安放，上身随绵长呼气自然微倾，给下腰留出温柔呼吸余地。 | `(0f, 0.15f, 0.65f, 0.65f, 0.15f, 28f, 28f, 0f, true)` | `(0f, 0.50f, 0.95f, 0.95f, -0.65f, 38f, 38f, 0f, true)` |
| 17 | `wrist_palm_forward_silk` | 前臂掌心朝外轻展 | `MOUSE_WRIST` | `40s` | `true` | 一臂平伸掌心朝外，另一手轻拢四指向回微引，舒展鼠标侧前臂内侧筋膜。 | `(0f, 0.10f, 0.65f, 0.60f, 0.10f, 68f, 78f, 0.45f, false)` | `(0f, 0.15f, 0.85f, 0.82f, 0.05f, 64f, 82f, 0.92f, true)` |
| 18 | `wrist_warm_water_wave` | 十指温水波浪舒张 | `MOUSE_WRIST` | `40s` | `false` | 像指尖拂过温水表面，随呼吸逐根舒展指节再轻柔松落，唤回指端温热。 | `(0f, 0.05f, 0.60f, 0.60f, 0.15f, 56f, 56f, 0.70f, true)` | `(0f, 0.15f, 0.88f, 0.88f, 0.05f, 42f, 42f, -0.55f, true)` |
| 19 | `wrist_prayer_lotus_turn` | 胸前合掌腕节轻转 | `MOUSE_WRIST`, `TRAPEZIUS_SHOULDER` | `40s` | `false` | 掌心于胸前温和相合，随呼气将前臂轻轻下沉半寸，松开敲字后的酸胀。 | `(0f, 0.10f, 0.55f, 0.55f, 0.20f, 72f, 72f, 0.50f, true)` | `(0f, 0.20f, 0.90f, 0.90f, 0.10f, 60f, 60f, 0.88f, true)` |
| 20 | `wrist_thumb_tendon_release` | 握拳包拇尺侧轻舒 | `MOUSE_WRIST` | `40s` | `true` | 四指轻拢拇指成柔拳，手腕向小指侧微倾，顺长呼气解开腕侧细微紧绷。 | `(0f, 0.08f, 0.60f, 0.60f, 0.12f, 52f, 52f, -0.25f, false)` | `(0f, 0.15f, 0.85f, 0.85f, 0.05f, 48f, 48f, -0.80f, true)` |
| 21 | `eye_warm_palming` | 搓热掌心温敷双目 | `EYE_BROW` | `40s` | `false` | 搓热双掌轻轻拢在眼眶周围，不压眼球，在温热静暗里听三声绵长呼吸。 | `(0f, 0.15f, 0.60f, 0.60f, 0.05f, 108f, 108f, 0.10f, true)` | `(0f, 0.28f, 0.88f, 0.88f, -0.08f, 102f, 102f, 0.0f, true)` |
| 22 | `eye_slow_orbit_mist` | 闭目凝神极慢转睛 | `EYE_BROW` | `40s` | `true` | 轻轻合上双眼，眼球如浮云般极慢画圆两周，把久盯屏幕的干涩缓缓化开。 | `(-3f, 0.05f, 0.65f, 0.65f, 0.15f, 14f, 14f, 0f, true)` | `(3f, 0.18f, 0.90f, 0.90f, 0.08f, 10f, 10f, 0f, true)` |
| 23 | `eye_brow_cuanzhu_smoothing` | 指腹轻拂攒竹眉心 | `EYE_BROW`, `POSTERIOR_NECK` | `40s` | `false` | 双手指腹由眉心向两侧太阳穴轻柔拂开，随呼气松开不自觉蹙起的眉头。 | `(0f, 0.10f, 0.58f, 0.58f, 0.10f, 112f, 112f, 0.15f, true)` | `(0f, 0.22f, 0.85f, 0.85f, 0.05f, 104f, 104f, 0.05f, true)` |
| 24 | `eye_horizon_soft_gaze` | 远山凝眸与柔眨润目 | `EYE_BROW`, `POSTERIOR_NECK` | `40s` | `false` | 目光越过屏幕投向远处窗外天际，轻眨三次后闭目小憩，让眼底重归清润。 | `(0f, -0.10f, 0.62f, 0.62f, 0.22f, 15f, 15f, 0f, false)` | `(0f, 0.12f, 0.90f, 0.90f, 0.10f, 12f, 12f, 0f, true)` |

### 2.2 三景情境主方、四枚救急轻帖与四册四时雅集映射表

1. **三档情境主方（`heroRoutineFor(scene)`）**：
   - `DESK_SEATED`（`home_scene_desk`）→ `hero_desk_seated`：**《午后伏案 · 沉肩松颈帖》**（`180s` = `shoulder_ear_melt 60s` + `shoulder_scapula_sigh 60s` + `neck_occiput_cradle 60s`，`seatedDiscreet = true`）
   - `TEA_BREAK_STANDING`（`home_scene_tea`）→ `hero_tea_break`：**《接水间隙 · 胸椎开阔帖》**（`150s` = `thoracic_desk_cat_cow 50s` + `thoracic_clasp_chest_open 50s` + `lumbar_willow_side_bend 50s`，`seatedDiscreet = false`）
   - `EVENING_BEDSIDE`（`home_scene_bed`）→ `hero_evening_bedside`：**《晚间沉香 · 腰背解绑帖》**（`360s` = `lumbar_bedside_butterfly_fold 60s` + `lumbar_willow_side_bend 60s` + `thoracic_seated_gentle_twist 60s` + `neck_fengchi_nod 60s` + `eye_warm_palming 60s` + `shoulder_double_inhale_sigh 60s`，`seatedDiscreet = true`）
2. **四枚片刻救急轻帖（`home_quick_routine_0..3`）**：
   - `home_quick_routine_0` → `quick_meeting_sigh`：**《连会余韵叹息》**（`90s` = `shoulder_double_inhale_sigh 45s` + `neck_fengchi_nod 45s`；同时作为 `EXTRA_FROM_REMINDER_QUICK_START` 直达套路）
   - `home_quick_routine_1` → `quick_screen_warm_eyes`：**《久盯屏幕温目》**（`120s` = `neck_thumb_fengchi_press 40s` + `eye_slow_orbit_mist 40s` + `eye_warm_palming 40s`）
   - `home_quick_routine_2` → `quick_mouse_wrist_relief`：**《鼠标手解缚》**（`120s` = `wrist_palm_forward_silk 40s` + `wrist_warm_water_wave 40s` + `wrist_prayer_lotus_turn 40s`）
   - `home_quick_routine_3` → `quick_sedentary_lumbar_wake`：**《久坐松腰唤醒》**（`180s` = `lumbar_pelvic_rock 45s` + `lumbar_seated_figure_four 45s` + `thoracic_seated_gentle_twist 45s` + `lumbar_willow_side_bend 45s`）
3. **四册永久免费四时雅集（`booklet_card_0..3`）**：
   - `booklet_card_0` → `booklet_neck_thoracic`：**《伏案颈胸开阔记》**（`totalDays = 7`，`dailyMinutes = 5`，温和改善圆肩含胸与长期低头后的颈后紧绷，随时暂歇续练亦无妨）
   - `booklet_card_1` → `booklet_lumbar_pelvis`：**《久坐腰骶归位帖》**（`totalDays = 7`，`dailyMinutes = 6`，针对久坐臀腰酸沉与骨盆僵滞的椅沿及桌边温和舒展）
   - `booklet_card_2` → `booklet_evening_breath`：**《晚间呼吸解绑》**（`totalDays = 7`，`dailyMinutes = 6`，以四吸六呼长节律配合全身轻柔松解，洗去日间疲惫安然入眠）
   - `booklet_card_3` → `booklet_eyes_wrist_care`：**《屏幕温目护腕册》**（`totalDays = 5`，`dailyMinutes = 4`，专为高频敲字与盯屏人群调配的眉眼清润及手腕前臂养护专册）
4. **无限次身体寻音配帖（`composeCustomRoutine(regions, targetMinutes)`）**：
   - 支持 `1..3` 个 `BodyRegion` 多选（若空选则温和回退至 `[POSTERIOR_NECK, TRAPEZIUS_SHOULDER]`）与 `2 / 4 / 6` 分钟目标时长。
   - 按选中部位主池轮询抽取体式，按 `10s` 完整呼吸周期将总时长精确分配至各体式（`totalDurationSeconds == targetMinutes * 60`），生成 `custom_<regions>_<minutes>m`，单日无限次免费调用。

---

## 3. 「开屏雅鉴」画心目录与七重频控状态机规范（`:feature:splashad`）

### 3.1 内置三幅离线东方器物画心（`CuratedHouseSplashAdProvider`）

| `id` | `artworkTheme` | `sponsorName` | `title` | `subtitle` | `ctaLabel` | `landingSummary` | `landingUrl` |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `house_celadon_tea` | `CELADON_TEA` | `山茗器物 · 东方茶席` | `案头一盏汝瓷温茶，陪你度过伏案午后` | `手作原矿天青釉主人杯 · 温润握感不烫手` | `了解器物详情` | `山茗器物春季茶席展：原矿天青釉案头杯与茶则细节一览（外部浏览器打开）。` | `https://example.com/songxi-showcase/celadon-tea` |
| `house_pine_incense` | `PINE_INCENSE` | `松间闻香 · 草木线香` | `一缕山松冷香，为书房留出片刻清净` | `古法柏木与冷杉合香 · 无化学助燃剂` | `探索草木香谱` | `松间闻香节气合香志：天然冷杉、柏木与艾草线香配方介绍（外部浏览器打开）。` | `https://example.com/songxi-showcase/pine-incense` |
| `house_wood_stand` | `WOOD_DESK_STAND` | `案头清风 · 木作工坊` | `把屏幕抬高半寸，让低垂的后颈自然归位` | `整块北美黑胡桃木榫卯桌面架 · 稳固温润` | `查看木作尺寸` | `案头清风人体工学木作：黑胡桃木显示器增高架与护腕掌托说明（外部浏览器打开）。` | `https://example.com/songxi-showcase/wood-stand` |

### 3.2 七重克制频控与强制预览判定顺序（`SplashAdPolicyEngine.evaluateLaunch`）

设当前时钟 `now = clock.nowEpochMillis()`，本地日期 `todayIso = clock.currentLocalDateTime().toLocalDate().toString()`：

| 判定序 | 条件表达式 | 返回决策 (`SplashGateDecision`) | 账本副作用 (`SplashAdLedgerRepository`) |
| ---: | :--- | :--- | :--- |
| **0 (预览覆盖)** | `forcePreview == true`（`EXTRA_FORCE_SPLASH_PREVIEW` 或设置页 `预览开屏雅鉴`） | `Show(creative, countdownSeconds = 3)` | 不受首启、单日上限或冷却间隔限制，立即展示 `3s` 开屏雅鉴 |
| **Gate 1** | `totalAppLaunchesAfterIncrement == 1 && warmBackgroundDurationMs == null` | `Bypass(FIRST_LAUNCH_GRACE)` | 冷启动计数 `0 -> 1`；首次安装启动零开屏打扰，直达「初见松息」 |
| **Gate 2** | `hasActiveOrPausedStretch == true` | `Bypass(ACTIVE_STRETCH_SESSION)` | 绝不打断正在进行或翻转暂存中的跟练会话 |
| **Gate 3** | `isReminderDeepLink == true`（`EXTRA_FROM_REMINDER_QUICK_START`） | `Bypass(REMINDER_QUICK_START)` | 免开屏直达 `quick_meeting_sigh`（`90s` 连会余韵叹息） |
| **Gate 4** | `warmBackgroundDurationMs != null && warmBackgroundDurationMs < 30 * 60 * 1000L` | `Bypass(SHORT_WARM_RESUME)` | 后台暂离 `< 30 分钟`（如 `29m`）免开屏；`>= 30 分钟`（如 `31m`）进入 Gate 5 |
| **Gate 5** | `impressionsOn(todayIso) >= 3` | `Bypass(DAILY_CAP_REACHED)` | 单自然日最多展示 `3` 次；跨零点进入新自然日自动清零额度 |
| **Gate 6** | `lastImpressionEpochMs != null && (now - lastImpressionEpochMs) < 4 * 60 * 60 * 1000L` | `Bypass(COOLDOWN_ACTIVE)` | 两次展示间隔严格 `>= 4 小时`（`+3h59m` 拦截，`+4h01m` 放行） |
| **Gate 7** | `withTimeoutOrNull(1200L) { provider.fetchCreative(personalized) } == null` | `Bypass(PROVIDER_TIMEOUT_OR_EMPTY)` | 超过 `1200ms` 未返回或无可用画心则静默跳过；成功返回时记录 `recordImpression` 并返回 `Show(creative, 3)` |

### 3.3 开屏交互与硬件红线不变量（`SplashAdScreen`）
1. **立轴比例**：上方 `78%` 宣绢内缩装裱画心（`splash_ad_backdrop`）+ 下方 `22%` 品牌印章底栏（`splash_ad_brand_footer`）。
2. **首帧可跳过**：`splash_ad_skip_button` 在 `t = 0ms` 即刻可见且可点击，触控热区严格 **`>= 48dp × 48dp`**（Web 原型 `>= 48px × 48px`），文案依次呈现 `跳过 3s` → `跳过 2s` → `跳过 1s`，满 `3000ms` 自动收起进入主界面。
3. **防误触与安全确认**：点击 `splash_ad_backdrop` 任意背景/插画处**完全静默无响应**；仅点击底部独立胶囊 `splash_ad_cta_button` 才会暂停倒计时并展开 `splash_ad_confirm_sheet`，点击 `splash_ad_confirm_dismiss` 安全关闭确认层并恢复倒计时。
4. **物理级禁绝摇一摇与静音保障**：`:feature:splashad` 不依赖 `:core:sensory`，运行期注册 **`0` 个 `SensorManager` 监听器**，且全程静音不播放音频。
