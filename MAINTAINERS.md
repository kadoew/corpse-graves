# دليل الصيانة — Corpse Graves

هذا المستودع يحتوي **22 مشروع Gradle** (9 Fabric + 8 Forge + 5 NeoForge قابلة للبناء) تنتج **23 جرّة**، لأن جرة NeoForge 1.20.1 هي نفس جرة Forge 1.20.1 (توافق ثنائي). راجع [SOURCE-MAP.md](SOURCE-MAP.md) لخريطة المصادر.

## مبدأ الإصلاح

1. **أعد إنتاج المشكلة** على أقل عدد من الإصدارات، واطلب من اللاعب: نسخة Minecraft، اللودر وإصداره، جرة المود، `latest.log`، وخطوات إعادة الإنتاج.
2. **حدد العائلة:** Fabric 26.x، Fabric legacy، Forge 26.x، Forge 1.21.x، NeoForge 26.x، أو NeoForge 1.21.x.
3. **أصلح في المشروع المرجعي للعائلة أولاً:**
   - Fabric 26.x: `fabric/26.3`
   - Fabric legacy: `fabric/1.21.1` (ثم 1.20.1 عند اختلاف API)
   - Forge 26.x: `forge/26.3`
   - Forge 1.21.x: `forge/1.21.1`
   - NeoForge 26.x: `neoforge/26.3`
   - NeoForge 1.21.x: `neoforge/1.21.1`
4. **مرّر الملفات المشتركة** إلى بقية إصدارات العائلة باستخدام السكربت، ثم عدّل ما يختلف في API يدوياً:
   ```powershell
   ./sync-shared.ps1 -FromProject fabric/26.3 -RelativePath "src/main/java/com/kador/corpsegraves/grave/GraveManager.java" -ToProjects fabric/26.2,fabric/26.1.2
   ```
5. **ابنِ الكل:**
   ```powershell
   $env:JAVA17_HOME="C:\path\to\jdk17"
   $env:JAVA21_HOME="C:\path\to\jdk21"
   $env:JAVA25_HOME="C:\path\to\jdk25"
   ./build-all.ps1
   ```
6. **اختبر:** سيرفر لكل عائلة على الأقل + عميل حقيقي للنسخة المتأثرة. لا تنشر إصلاحاً غير مختبر.
7. **حدّث الإصدار** في `gradle.properties` لكل مشروع متأثر، ثم أعد البناء، وحدّث `docs/downloads` والبصمات والحزمة، ثم أنشئ Release جديداً.

## الملفات المشتركة والنسخ الخاصة

- `GraveMenu.java` متطابق في معظم المشاريع؛ الإصلاح فيه ينتقل بسهولة.
- `GraveManager.java` يختلف بين العائلات وبعض إصدارات 26.x؛ لا تنسخه أعمى. راجع الفرق:
  ```powershell
  git diff --no-index fabric/26.3/src/.../GraveManager.java fabric/26.1/src/.../GraveManager.java
  ```
- Fabric 26.1 يستخدم `EntityType`، بينما 26.2+ يستخدم `EntityTypes`. لا توحّد هذا السطر.
- Forge 26.x يستخدم EventBus 7 و`ChannelBuilder.optional()`؛ Forge 1.21.x يستخدم EventBus 6. لا تنقل إصلاحاً بينهما دون تعديل.
- NeoForge 26.x يستخدم `DeferredHolder` و`RegisterPayloadHandlersEvent`؛ لا تنسخ كود Forge إليه.
- NeoForge 1.20.1 ليس مشروعاً مستقلاً؛ ابنِ `forge/1.20.1` وسمّ الجرة `...-neoforge.jar` عند الحاجة.

## مدة الإصلاح المتوقعة

- إصلاح منطق مشترك + تمريره + بناء الـ23 + اختبار دخان: عادة **1–3 ساعات** في جلسة واحدة.
- إصلاح مرتبط بتغيير API في عائلة واحدة: قد يضيف **30–90 دقيقة** لكل عائلة.
- لا يعني تعدد النسخ أن كل نسخة تحتاج يوماً منفصلاً؛ البناء آلي، لكن **الاختبار البشري** هو ما يستهلك الوقت.
- إذا كان العطل في نسخة واحدة فقط، يمكن إصدار إصلاح لتلك النسخة وحدها بسرعة، ثم تعميمه.

## قائمة ما قبل الإصدار

- [ ] الإصلاح مثبت في المصدر المرجعي.
- [ ] `build-all.ps1` ناجح لكل المشاريع.
- [ ] سيرفرات العائلات المتأثرة تصل `Done`.
- [ ] اختبار عميل للنسخة المتأثرة (أو توثيق عدم توفر الاختبار).
- [ ] تحديث `gradle.properties` و`docs/downloads` والبصمات والحزمة.
- [ ] تحديث `README.md`/`docs/index.html` عند تغيير الإصدار أو الميزات.
- [ ] إنشاء GitHub Release ورفع الجرار والحزمة.
- [ ] فتح/إغلاق Issue وذكر التغييرات في الـchangelog.

## نصائح صيانة

- لا ترفع `build/` أو `run/` أو `logs/` أو العوالم أو التوكنات إلى Git.
- احتفظ بوسم Git لكل إصدار (`v1.1.0`, `v1.1.1`...) حتى يمكن الرجوع.
- استخدم Issue template الموجود في `.github/ISSUE_TEMPLATE`.
- أي إصلاح أمني أو فقدان بيانات: أصدر تصحيحاً سريعاً لكل العائلات المتأثرة، ووضّح ذلك في README.
