# البناء من المصدر

## المتطلبات

| العائلة | Java | ملاحظة |
|---|---|---|
| Fabric 26.x | 25 | Loom يطلب JDK 25 |
| Fabric 1.21 / 1.21.1 | 21 | |
| Fabric 1.20.1 | 17 | |
| Forge 26.x | 25 | ForgeGradle 7 |
| Forge 1.21.x | 21 | ForgeGradle 7 / EventBus 6 |
| Forge 1.20.1 | 17 | ForgeGradle 6 |
| NeoForge 26.x | 25 | MDG 2 |
| NeoForge 1.21.x | 21 | MDG 2 |

- استخدم الـwrapper المرفق داخل كل مشروع؛ لا تحتاج تثبيت Gradle عالمياً.
- أول بناء قد ينزّل Minecraft/Loom/MDG، لذلك يحتاج إنترنت ووقتاً أطول.

## بناء مشروع واحد

```powershell
cd fabric/26.3
$env:JAVA_HOME="C:\path\to\jdk25"
./gradlew.bat build --console=plain
```

النتيجة في `build/libs/*.jar`. لا ترفع مجلد `build/` إلى Git.

أمثلة:

```powershell
cd fabric/1.20.1; $env:JAVA_HOME="C:\path\to\jdk17"; ./gradlew.bat build
cd forge/26.3;     $env:JAVA_HOME="C:\path\to\jdk25"; ./gradlew.bat build
cd neoforge/26.3;  $env:JAVA_HOME="C:\path\to\jdk25"; ./gradlew.bat build
```

## بناء كل المشاريع

```powershell
$env:JAVA17_HOME="C:\path\to\jdk17"
$env:JAVA21_HOME="C:\path\to\jdk21"
$env:JAVA25_HOME="C:\path\to\jdk25"

./build-all.ps1 -List          # عرض المشاريع فقط
./build-all.ps1                # بناء كل المشاريع
./build-all.ps1 -Only fabric   # بناء عائلة واحدة
```

- يجمع السكربت الجرار في `artifacts/<العائلة>-<الإصدار>/`.
- ينشئ `artifacts/SHA256SUMS.txt`.
- ينسخ جرة Forge 1.20.1 كتوافق NeoForge 1.20.1 إلى `artifacts/neoforge-1.20.1/` إن وُجدت.

## NeoForge 1.20.1

لا يوجد مشروع NeoForge 1.20.1 مستقل؛ ابنِ `forge/1.20.1` ثم أعد تسمية الجرة إلى:

```
corpsegraves-1.1.0+1.20.1-neoforge.jar
```

هذه الجرة جُرِّبت على NeoForge 47.1.106 بعميل Vanilla.

## المشاكل الشائعة

- **`Unsupported class file major version`**: استخدم JDK الصحيح للعائلة.
- **فشل تنزيل POM لإصدار NeoForge**: بعض إصدارات 26.1.x غير منشورة أصلاً؛ راجع SOURCE-MAP.
- **Forge 26.x لا يبني مع FG6**: استخدم ForgeGradle 7 والـwrapper المرفق.
- **بطء أول بناء**: طبيعي؛ شغّل البناء مرة أخرى لقياس الزمن الحقيقي.

## التحقق قبل النشر

1. شغّل سيرفراً مخصصاً بكل عائلة متأثرة وتأكد من `Done`.
2. نفّذ موتاً تجريبياً وتأكد من القبر والهولوغرام.
3. اختبر عميلاً حقيقياً عند تغيير الواجهة أو العرض.
4. حدّث `docs/downloads` والبصمات والحزمة عبر `build-all.ps1` أو يدوياً.
