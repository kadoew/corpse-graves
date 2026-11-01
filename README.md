# Corpse Graves

**لا تفقد غنائمك بعد الموت.** عند موت اللاعب تُحفظ أغراضه وخبرته داخل قبر محمي، بجسد يعرض سكين اللاعب الحقيقي، وهولوغرام عائم يعرض الاسم ومؤقت الاختفاء ومؤقت/حالة الحماية.

> **تنبيه:** هذا مود غير رسمي لماين كرافت Java Edition، وغير مرتبط بـ Mojang أو Microsoft. جرّبه أولاً على عالم/سيرفر اختباري واحتفظ بنسخة احتياطية. رخصة المشروع MIT.

## الميزات

- سكين اللاعب الحقيقي على الجسد (Mannequin في 26.x، وجسد مخصص في النسخ الأقدم).
- حماية المالك، مؤقت فتح للآخرين، وتجاوز للمشرفين.
- هولوغرام حي: الاسم، وقت الاختفاء، وحالة/وقت الحماية بألوان قابلة للتعديل.
- واجهة مخصصة بزرّي **Take All** و **Restore**، و **Shift + كليك** لاستعادة كل شيء إلى خاناته الأصلية.
- خبرة 50% افتراضياً (0–100 قابلة للتعديل).
- اختفاء ذكي، إيقاف المؤقت أثناء غياب المالك أو إغلاق التشك، وبقاء البيانات بعد إعادة التشغيل.
- إنقاذ الفراغ، دعم curse of vanishing، وحد أقصى للقبور لكل لاعب.
- إعدادات JSON عميقة: المؤقتات، الألوان، حجم/ارتفاع الهولوغرام، وضعية الجسد، الخبرة، وإنقاذ الفراغ.
- أوامر إدارة: `locate`، `reload`، `unlock`، `remove`، `open`، `restore`.

## الإصدارات المدعومة (23 جرّة)

| اللودر | الإصدارات | العدد |
|---|---|---|
| Fabric | 26.1، 26.1.1، 26.1.2، 26.2، 26.3، 26.4-alpha.6، 1.21.1، 1.21، 1.20.1 | 9 |
| Forge | 1.20.1، 1.21، 1.21.1، 26.1، 26.1.1، 26.1.2، 26.2، 26.3 | 8 |
| NeoForge | 1.20.1 (توافق)، 1.21، 1.21.1، 26.1.2، 26.2، 26.3 | 6 |

**حدود المنبع:** لا يوجد Forge أو NeoForge لماين كرافت 26.4، ولا إصدارات NeoForge منشورة لـ26.1/26.1.1. نسخة NeoForge 1.20.1 هي جرة التوافق الثنائي مع فرع Forge 47.1.x. التفاصيل الكاملة في [SOURCE-MAP.md](SOURCE-MAP.md).

## التحميل

- **GitHub Releases:** `https://github.com/YOUR_USERNAME/corpse-graves/releases` — ارفع الجرار والحزمة هنا بعد إنشاء المستودع.
- **صفحة GitHub Pages:** `https://YOUR_USERNAME.github.io/corpse-graves/` — تُنشر من مجلد `docs/` بعد تفعيل Pages.
- **مرايا مؤقتة/دائمة حالياً:**
  - صفحة الإصدار: https://telegra.ph/Corpse-Graves-110--تحميل-Fabric-وForge-وNeoForge-11-01
  - الحزمة الكاملة: https://files.catbox.moe/h6miy2.zip
    `SHA-256: bf201d761d61fd7adb9e57788db9da347fb8582fc1ba3b9fe504bb2c37ca3586`

> بعد إنشاء مستودعك، استبدل `YOUR_USERNAME` في هذا الملف وفي `docs/` إن لزم.

## التثبيت

### Fabric
1. ثبّت Fabric Loader المناسب: 0.19.3+ لـ26.x، و0.16+ لـ1.21.x، و0.15+ لـ1.20.1.
2. ضع جرة المود مع إصدار **Fabric API** المطابق في مجلد `mods` على العميل والسيرفر.
3. استخدم Java 25 لـ26.x، وJava 21 لـ1.21/1.21.1، وJava 17 لـ1.20.1.

### Forge / NeoForge
1. ثبّت إصدار اللودر المطابق (Forge 47.x/51/52/62/63/64/65/66، NeoForge 47.1.106/21.0.167/21.1.176/26.1.2.125/26.2.0.93/26.3.0.100).
2. ضع الجرة فقط في `mods` على العميل والسيرفر. لا تحتاج Fabric API.
3. لا تخلط جرة Fabric مع Forge أو NeoForge، والعكس صحيح.

**مهم:** لا تضع أكثر من جرة Corpse Graves واحدة في `mods`؛ وجود أكثر من نسخة بنفس معرف المود يسبب خطأ.

## الإعدادات

الملف: `config/corpsegraves.json` (نموذج في `docs/downloads/corpsegraves.sample.json`).

```json
{
  "despawnSeconds": 3600,
  "unlockSeconds": 1800,
  "xpRecoveryPercent": 50.0,
  "hologram": {
    "scale": 1.4,
    "height": 1.45,
    "background": "#66000000",
    "nameColor": "#FFAA00",
    "despawnColor": "#55FFFF",
    "protectedColor": "#FF5555",
    "unlockedColor": "#55FF55"
  }
}
```

بعد التعديل: `/corpsegraves reload`.

## البناء من المصدر

- Java 17 لـ1.20.1، Java 21 لـ1.21.x، Java 25 لـ26.x.
- استخدم الـwrapper المرفق داخل كل مشروع: `./gradlew build` (أو `gradlew.bat` على Windows).
- لبناء كل المشاريع: `pwsh ./build-all.ps1` بعد ضبط `JAVA17_HOME` و`JAVA21_HOME` و`JAVA25_HOME`.
- التفاصيل في [BUILDING.md](BUILDING.md)، وخطة الصيانة في [MAINTAINERS.md](MAINTAINERS.md).

## بنية المستودع

```
fabric/<version>/       مشاريع Fabric (26.3 هو المرجع الأساسي للعائلة الحديثة)
forge/<version>/        مشاريع Forge لكل إصدار
neoforge/<version>/     مشاريع NeoForge لكل إصدار منشور
docs/                   الموقع الجاهز لـGitHub Pages (يشمل الجرار والإعدادات والبصمات)
build-all.ps1           بناء كل المشاريع وجمع الجرار
MAINTAINERS.md          خريطة المصادر، تمرير الإصلاحات، واختبارات الإصدار
SOURCE-MAP.md           مصدر كل مجلد والحالات المعروفة
PUBLISH-GITHUB.md       خطوات النشر على GitHub وPages بالتفصيل
```

## الاختبار والحدود

- جميع السيرفرات الـ23 تصل `Done` وتنشئ قبوراً.
- فحص كامل بعميل حقيقي: Fabric 26.3/1.21.1/1.20.1، Forge 26.3/1.20.1، NeoForge 26.3/1.21.1.
- عميل Vanilla (Quick Loot): Forge 1.21/1.21.1/26.3، وNeoForge 1.20.1 (3 ستاكات + 698 خبرة).
- بقية نسخ 26.x جُرِّبت على سيرفرات مخصصة فقط (تحميل + إنشاء قبور)، دون فحص بصري كامل.

## المساهمة

- افتح **Issue** قبل التغيير الكبير، واذكر نسخة اللعبة واللودر وأحدث `latest.log`.
- استخدم [.github/ISSUE_TEMPLATE/bug_report.yml](.github/ISSUE_TEMPLATE/bug_report.yml).
- التزم برخصة MIT، ولا ترفع عوالم أو سجلات لاعبين أو مفاتيح/توكنات.

## الرخصة

MIT — انظر [LICENSE](LICENSE). حقوق النشر (c) 2026 Kador.

---

## English

Corpse Graves is an unofficial Minecraft Java mod that stores death loot and XP in a protected, skin-accurate corpse grave with a hologram, owner unlock timer and a Take All/Restore GUI. It supports 9 Fabric, 8 Forge and 6 NeoForge builds (23 jars total). Source and site are MIT-licensed. Build instructions are in [BUILDING.md](BUILDING.md); release/maintenance workflow is in [MAINTAINERS.md](MAINTAINERS.md). Not affiliated with Mojang or Microsoft.
