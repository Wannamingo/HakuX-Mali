# HakuX-Mali

[Русский](README.md) | [English](README.en.md)

![HakuX-Mali](assets/branding/hakux-mali-banner.png)

Форк Xbox-эмулятора [hakuX](https://github.com/rfandango/hakuX) для Android с Mali. **Единый релиз 1.4/code22 поддерживает ANBERNIC RG477M, RG Vita Pro и RG Rotate.** Основа — hakuX 1.2.0.

**[Релиз 1.4: APK, исходники и контрольные суммы](https://github.com/Wannamingo/HakuX-Mali/releases/tag/v1.4)** · **[Подробное описание всех нововведений](releases/1.4.ru.md)**

## Новое в 1.4

- Измерительный автоподбор: несколько конфигураций из одной временной точки, реальные FPS/P95, применение результата для игры и восстановление при отмене/сбое.
- Автоматический профиль с сохранением ручных значений и экономичный профиль 30 FPS.
- Громкость, отключение звука, три варианта буферизации и HRTF.
- Обрезка краёв, увеличение и смещение изображения, выбор частоты дисплея; сохранены 4:3 / 16:9.
- Автосохранение при штатном выходе, предложение продолжить, отдельный быстрый слот и сочетания Select + кнопка.
- Чувствительность/инверсия стиков, сила вибрации, экспорт и импорт настроек.
- Предупреждения о температуре; прежний однострочный FPS/frametime/temperature HUD.
- Syberia: дополнено исправление фона меню для прямого доступа к памяти на Rotate.

## Поддерживаемые консоли

| Консоль | Платформа | С чего начать |
|---|---|---|
| RG477M | Dimensity 8300 / Mali-G615 | Встроенный MaliSX2 0.2.3, 1×, Bilinear, FXAA Off |
| RG Vita Pro | RK3576 / Mali-G52, Android 14 / GammaOS Next Lite | System driver, 1×, Bilinear, FXAA Off |
| RG Rotate | T618 / Mali-G52, Android 12 | System driver / Vulkan 1.1, 1×, Bilinear, FXAA Off |

На RG477M выполнен полный измерительный цикл 1.4: в проверенной сцене Syberia 1× — около 60 FPS, 2× — около 48. На Rotate в линии 1.4 проверен уровень Syberia; в итоговом APK — исправленное меню Syberia и уровень Serious Sam, около 60 FPS на снятых кадрах. Для Vita Pro 1.4 установлена с сохранением данных до дополнения Rotate; замеры Most Wanted относятся к прежней сборке: System 1× — 30 FPS, System 2× — 29,9 FPS. Это разные проверки, не единый сравнительный бенчмарк всех устройств.

[Автоподбор и методика](docs/MEASURED-CALIBRATION.ru.md) · [Измерения RG477M](docs/CALIBRATION-RG477M-1.4.ru.md) · [Тесты и температуры Vita Pro](docs/RG-VITA-PRO-PERFORMANCE.ru.md) · [Галерея 1.4](docs/screenshots/1.4/README.md)

## Установка

1. Установите **HakuX-Mali-1.4-release.apk** поверх нашей предыдущей версии. Package ID и подпись сохранены; настройки и состояния не требуют удаления приложения.
2. Выберите папку собственных ISO/XISO или игр с `default.xbe`. Архивы исходников релиза предназначены для разработчиков.
3. На RG477M встроен MaliSX2; при обновлении явный выбор драйвера сохраняется. На Rotate проверен системный драйвер, а не MaliSX2. Перезапустите игру после смены драйвера.
4. Авто по умолчанию выключено. Для подбора выберите стабильную игровую сцену и пункт меню паузы «Подобрать настройки по замерам».
5. Потолок новой установки — 120 FPS; прежнее ручное значение сохраняется. Собственные ограничения игр 30/60 FPS остаются.

Встроенные обновления используют **Wannamingo/HakuX-Mali**. Для ES-DE: Microsoft Xbox → HakuX-Mali (Standalone), [конфигурации](https://github.com/Wannamingo/es-de-android-custom-systems). Сначала выдайте приложению доступ к той же папке игр.

## Serious Sam и русская озвучка

Для папочной T-Rus Ryudo сохранена [инструкция обхода зависающего загрузчика](docs/SERIOUS-SAM.ru.md): резервная копия исходного `default.xbe`, затем копия `eng.xbe` как `default.xbe`. На RG Rotate достигнут игровой уровень; **владелец подтвердил, что русская озвучка сохранилась**. Русский текст отдельно не проверялся. Приложение само файлы игры не переименовывает.

## Исправления RG477M

- Импорт и загрузка MaliSX2 на RG477M с проверкой фактически выбранного драйвера и явной ошибкой загрузки.
- Очередь из 64 Vulkan fences и совместимый вывод кадров для драйверов без timeline semaphores.
- Syberia и Ultimate Beach Soccer: исправлен ранний выход при запуске файлового образа через обработку XBE media flags в памяти.
- Serious Sam: доставка гостевых SEH-исключений и сохранение прогресса REP MOVS устраняют вылет при запуске.
- Ultimate Spider-Man: сохранены атрибуты PAGE_WRITECOMBINE; использован расширенный вариант исправления памяти из 1.2.0 с MmSetAddressProtect и совместимым MM v2.
- Serious Sam: незавершённые/недоступные запросы видимости больше не дают ложный ноль; устранены чёрные помехи и сильная задержка управления.
- Warrior Within: линейная видеотекстура выбирается по pitch, размеру пикселя и размерам; исправлены полосы и растяжение нижней части видеовставок.
- The Two Thrones: невыровненные массивы вершин перепаковываются без изменения значений; исправлены лишние части модели и треугольники.
- The Sands of Time: точные границы текстур свечения убирают зелёную полосу справа, сохраняя сам эффект.
- The Sands of Time / MaliSX2 0.2.3: разделение legacy render pass при смене blending/write mask и синхронизация вложений устраняют чёрные мерцающие квадраты в небе и на полу.
- Лимиты FPS: без ограничения, 30, 60, 90, 120. На проверенной консоли сохранено 120 FPS; ограничитель не накапливает очередь кадров и не отменяет собственный лимит игры.
- Release HUD содержит одну строку FPS, среднего/последнего времени кадра и доступных температур CPU/GPU/батареи. Нижние диагностические строки и временные режимы экспериментов отсутствуют.
- Новая иконка из предоставленного изображения: обычная, адаптивная и монохромная версии для пяти плотностей, с сохранением пропорций.
- Перенос на hakuX 1.2.0: сохранены новые исправления звука, памяти, стеков, контроллеров и System Link. Флаги present_cpu_wait и rop_pass_split разделены; fpslimit перенесён в общий parser; hook-библиотеки adrenotools собраны согласованно.

Полное [описание релиза](releases/1.4.ru.md) и [технические детали](android/RG477M.md).

## Успешно запущенные игры на RG477M

**31 подтверждённый запуск**: 29 образов подтверждены владельцем 27.10.2026, Syberia — предыдущей проверкой, Call of Cthulhu — игровым уровнем на .2 28.10.2026. Это не подтверждение полного прохождения или отсутствия всех дефектов; прежние 30 игр не перепроверялись целиком на .2. [Область проверок и источники подтверждения](docs/COMPATIBILITY.md).

- Call of Cthulhu: Dark Corners of the Earth — игровой уровень работает; во вступительном ролике остаются пропавшие текстуры.
- Colin McRae Rally 2005
- Dead or Alive Xtreme Beach Volleyball
- Jet Set Radio Future
- Manhunt
- Midnight Club 3: DUB Edition
- Need for Speed: Carbon
- Need for Speed: Hot Pursuit 2
- Need for Speed: Most Wanted – Black Edition
- Need for Speed: Underground 2
- OutRun 2006: Coast 2 Coast
- Pinball Hall of Fame: The Gottlieb Collection
- Prince of Persia: The Sands of Time
- Prince of Persia: The Two Thrones
- Prince of Persia: Warrior Within
- Pro Evolution Soccer 5
- Pro Tennis WTA Tour (Europe)
- Rayman 3: Hoodlum Havoc
- Serious Sam
- Serious Sam II
- Soulcalibur II
- Spyro: A Hero’s Tail
- SSX on Tour
- Street Racing Syndicate (SRS)
- Syberia
- Tennis Masters Series 2003
- Ultimate Beach Soccer
- Ultimate Pro Pinball
- Ultimate Spider-Man
- WTA Tour Tennis (USA)
- XGRA: Extreme G Racing Association


Проверка 30.10.2026: **Manhunt** загружается напрямую из папки с 969 файлами; достигнут игровой уровень, около **50 FPS** в PAL-версии. Полное прохождение не проверялось.

## Известные ограничения

Дефект текстур вступительного ролика Call of Cthulhu пока не исправлен. Качество всех комикс-сцен Ultimate Spider-Man и длительные игровые сессии не проверены. Conker: Live & Reloaded пока не включена в подтверждённые игровые проверки. Dimensity 8300/8300 Ultra распознаётся по MT6897, но фактическое тестирование проведено на RG477M.

## Сборка и исходники

См. [android/README.md](android/README.md) и [android/RG477M.md](android/RG477M.md). Обычный GitHub ZIP не включает содержимое submodules; в релизе приложен отдельный архив исходников с FEX, использованными native-зависимостями, лицензиями и манифестом ревизий. Для adrenotools включён необходимый upstream-патч 1.2.0.

## Лицензия и авторство

hakuX — свободное ПО по **GPLv3 или более поздней версии**: [COPYING](COPYING), [LICENSE](LICENSE), [licenses/](licenses/). Основной проект: [rfandango/hakuX](https://github.com/rfandango/hakuX); FEX fork и APU на основе xemu сохраняют своё авторство и лицензии. Этот форк не связан с Microsoft или ANBERNIC. Игры, BIOS, HDD, пользовательские данные и закрытые ключи подписи не распространяются.
