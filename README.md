# HakuX for RG477M

[Русский](README.md) | [English](README.en.md)

<img src="android/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="128" alt="HakuX for RG477M icon">

Бесплатная неофициальная адаптация [hakuX](https://github.com/rfandango/hakuX) для **ANBERNIC RG477M / Mali-G615 MC6**, Android arm64 и **MaliSX2 0.2.3**. Основа текущего релиза — hakuX 1.2.0. Авторство основного проекта и лицензии компонентов сохранены.

Подготовленная сборка **1.2.0-rg477m.2** уже установлена для проверки на RG477M: встроенный MaliSX2, распознавание Dimensity 8300, обновления нашего форка и проверенный переключатель 4:3/16:9. [Описание сборки](releases/1.2.0-rg477m.2.ru.md). На GitHub она пока не опубликована; инструкция ниже относится к опубликованной .1.

## Скачать

**[1.2.0-rg477m.1 — APK, исходники и контрольные суммы](https://github.com/Wannamingo/hakuX-for-RG477M/releases/tag/v1.2.0-rg477m.1)**

Публикуемый APK совпадает с версией, установленной и проверенной на RG477M. Это оптимизированная release-сборка arm64-v8a, без режима отладки. Package ID `com.rfandango.haku_x.debug` сохранён для обновления нашей предыдущей установки; суффикс не включает отладку.

## Установка

1. Установите APK из Releases. Поверх нашей предыдущей версии он сохраняет данные и настройки.
2. При первой установке завершите настройку приложения и выберите папку собственных Xbox ISO/XISO.
3. В Settings → GPU driver импортируйте официальный [MaliSX2 0.2.3](https://github.com/bmdhacks/malisx2/releases/tag/v0.2.3) и выберите его. Перезапустите игру после смены драйвера.
4. Для новой установки выберите Settings → Frame rate limit → **120 FPS**. На уже проверенной консоли это значение сохранено. Игры с внутренним лимитом 30/60 FPS сохраняют его.
5. При включённом FPS HUD показывает только FPS и среднее/последнее время кадра.

Обновления этой версии форка устанавливаются из его Releases; встроенная проверка обновлений в данном APK ещё обращается к основному hakuX.

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
- Release HUD содержит одну строку FPS и среднего/последнего времени кадра. Нижние диагностические строки и временные режимы экспериментов отсутствуют.
- Новая иконка из предоставленного изображения: обычная, адаптивная и монохромная версии для пяти плотностей, с сохранением пропорций.
- Перенос на hakuX 1.2.0: сохранены новые исправления звука, памяти, стеков, контроллеров и System Link. Флаги present_cpu_wait и rop_pass_split разделены; fpslimit перенесён в общий parser; hook-библиотеки adrenotools собраны согласованно.

Полное [описание релиза](releases/1.2.0-rg477m.1.ru.md) и [технические детали](android/RG477M.md).

## Успешно запущенные игры

Владелец подтвердил все 29 образов в текущей папке игр; Syberia подтверждена предыдущей проверкой. **30 позиций** перечислены ниже. На новой основе отдельно перепроверена The Sands of Time. [Область проверок и источники подтверждения](docs/COMPATIBILITY.md).

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

## Сборка и исходники

См. [android/README.md](android/README.md) и [android/RG477M.md](android/RG477M.md). Обычный GitHub ZIP не включает содержимое submodules; в релизе приложен отдельный архив исходников с FEX, использованными native-зависимостями, лицензиями и манифестом ревизий. Для adrenotools включён необходимый upstream-патч 1.2.0.

## Лицензия и авторство

hakuX — свободное ПО по **GPLv3 или более поздней версии**: [COPYING](COPYING), [LICENSE](LICENSE), [licenses/](licenses/). Основной проект: [rfandango/hakuX](https://github.com/rfandango/hakuX); FEX fork и APU на основе xemu сохраняют своё авторство и лицензии. Этот форк не связан с Microsoft или ANBERNIC. Игры, BIOS, HDD, пользовательские данные и закрытые ключи подписи не распространяются.
