# خريطة المصادر والإصدارات

## المشاريع (22 مشروع بناء)

| المجلد | المصدر | Java | الحالة |
|---|---|---|---|
| `fabric/26.3` | `corpsegraves` | 25 | المرجع الأساسي؛ اختبار كامل بعميل |
| `fabric/26.4` | `versions/cg26.4` | 25 | سيرفر OK (snapshot 6) |
| `fabric/26.2` | `versions/cg26.2` | 25 | سيرفر OK |
| `fabric/26.1.2` | `versions/cg26.1.2` | 25 | سيرفر OK |
| `fabric/26.1.1` | `versions/cg26.1.1` | 25 | سيرفر OK |
| `fabric/26.1` | `versions/cg26.1` | 25 | سيرفر OK (`EntityType`) |
| `fabric/1.21.1` | `corpsegraves-1.21.1` | 21 | عميل حقيقي |
| `fabric/1.21` | `versions/cg1.21` | 21 | سيرفر + هيت بوكس |
| `fabric/1.20.1` | `corpsegraves-1.20.1` | 17 | عميل حقيقي |
| `forge/26.3` | `corpsegraves-forge-26.3` | 25 | عميل حقيقي + Take All |
| `forge/26.2` | `corpsegraves-forge-26.2` | 25 | سيرفر OK |
| `forge/26.1.2` | `corpsegraves-forge-26.1.2` | 25 | سيرفر OK |
| `forge/26.1.1` | `corpsegraves-forge-26.1.1` | 25 | سيرفر OK |
| `forge/26.1` | `corpsegraves-forge-26.1` | 25 | سيرفر OK |
| `forge/1.21.1` | `corpsegraves-forge-1.21.1` | 21 | عميل Vanilla |
| `forge/1.21` | `corpsegraves-forge-1.21` | 21 | عميل Vanilla |
| `forge/1.20.1` | `corpsegraves-forge-1.20.1` | 17 | عميل حقيقي + حماية لاعبين |
| `neoforge/26.3` | `corpsegraves-neoforge-26.3` | 25 | عميل حقيقي + Take All |
| `neoforge/26.2` | `corpsegraves-neoforge-26.2` | 25 | سيرفر OK |
| `neoforge/26.1.2` | `corpsegraves-neoforge-26.1.2` | 25 | سيرفر OK |
| `neoforge/1.21.1` | `corpsegraves-neoforge-1.21.1` | 21 | عميل حقيقي |
| `neoforge/1.21` | `corpsegraves-neoforge-1.21` | 21 | سيرفر OK |

## الجرة الثالثة والعشرون

`neoforge/1.20.1` ليس مشروع بناء. استخدم جرة Forge 1.20.1 وأعد تسميتها `...-neoforge.jar`؛ هي متوافقة ثنائياً مع NeoForge 47.1.106 وجُرِّبت بعميل Vanilla.

## فجوات المنبع (غير قابلة للبناء)

- **Minecraft 26.4:** لا يوجد Forge ولا NeoForge في Maven؛ Fabric فقط.
- **NeoForge 26.1 / 26.1.1:** لم تُنشر إصداراتها (POM 404)؛ NeoForge يبدأ من 26.1.2.
- **NeoForge 1.20.1 الحديث:** الفرع الوحيد هو 47.1.x القديم؛ الجرة المتوافقة كافية.

لا تُضف جرة وهمية لهذه الفجوات. وثّقها في README عند كل إصدار.
