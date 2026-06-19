package com.example.kvantroium.features.pixels

enum class PixelColumnMode {
    Fixed,
    Select,
    Number,
    Penalty,
    Readonly,
    ReadonlyTotal
}

data class PixelSelectOption(
    val id: String,
    val label: String,
    val points: Int
)

data class PixelColumnDef(
    val key: String,
    val label: String,
    val mode: PixelColumnMode,
    val fixedPoints: Int? = null,
    val options: List<PixelSelectOption> = emptyList(),
    val hint: String? = null
)

val EDITABLE_PIXEL_KEYS = listOf(
    "part_of_comp",
    "make_content",
    "invite_friend",
    "clean_kvantum",
    "filled_project_card_on_time",
    "finished_project_with_product",
    "regional_competition",
    "interregional_competition",
    "all_russian_competition",
    "international_competition",
    "nto",
    "become_an_engineering_volunteer",
    "help_with_event",
    "make_own_event",
    "special_achievements",
    "fine"
)

val PIXEL_CRITERIA_COLUMNS = listOf(
    PixelColumnDef(
        key = "part_of_comp",
        label = "Участие в конкурсах от технопарка",
        mode = PixelColumnMode.Fixed,
        fixedPoints = 20
    ),
    PixelColumnDef(
        key = "make_content",
        label = "Создание контента для соцсетей",
        mode = PixelColumnMode.Fixed,
        fixedPoints = 20
    ),
    PixelColumnDef(
        key = "invite_friend",
        label = "Приведи друга в технопарк",
        mode = PixelColumnMode.Fixed,
        fixedPoints = 30
    ),
    PixelColumnDef(
        key = "clean_kvantum",
        label = "Уборка в квантуме",
        mode = PixelColumnMode.Fixed,
        fixedPoints = 10
    ),
    PixelColumnDef(
        key = "filled_project_card_on_time",
        label = "Вовремя заполнил проектную карту",
        mode = PixelColumnMode.Fixed,
        fixedPoints = 30
    ),
    PixelColumnDef(
        key = "finished_project_with_product",
        label = "Закрыл проект итоговым продуктом",
        mode = PixelColumnMode.Fixed,
        fixedPoints = 100
    ),
    PixelColumnDef(
        key = "regional_competition",
        label = "Региональный конкурс",
        mode = PixelColumnMode.Select,
        options = listOf(
            PixelSelectOption("member", "Участник (+20)", 20),
            PixelSelectOption("winner", "Призёр / победитель (+40)", 40)
        )
    ),
    PixelColumnDef(
        key = "interregional_competition",
        label = "Межрегиональный конкурс",
        mode = PixelColumnMode.Select,
        options = listOf(
            PixelSelectOption("member", "Участник (+30)", 30),
            PixelSelectOption("winner", "Призёр / победитель (+60)", 60)
        )
    ),
    PixelColumnDef(
        key = "all_russian_competition",
        label = "Всероссийский конкурс",
        mode = PixelColumnMode.Select,
        options = listOf(
            PixelSelectOption("member", "Участник (+50)", 50),
            PixelSelectOption("winner", "Призёр / победитель (+100)", 100)
        )
    ),
    PixelColumnDef(
        key = "international_competition",
        label = "Международный конкурс",
        mode = PixelColumnMode.Select,
        options = listOf(
            PixelSelectOption("member", "Участник (+75)", 75),
            PixelSelectOption("winner", "Призёр / победитель (+150)", 150)
        )
    ),
    PixelColumnDef(
        key = "nto",
        label = "НТО",
        mode = PixelColumnMode.Select,
        options = listOf(
            PixelSelectOption("member", "Участник (+50)", 50),
            PixelSelectOption("winner", "Призёр / победитель (+100)", 100)
        )
    ),
    PixelColumnDef(
        key = "become_an_engineering_volunteer",
        label = "Стать инженерным волонтёром",
        mode = PixelColumnMode.Fixed,
        fixedPoints = 30
    ),
    PixelColumnDef(
        key = "help_with_event",
        label = "Помощь в проведении мероприятия",
        mode = PixelColumnMode.Fixed,
        fixedPoints = 30
    ),
    PixelColumnDef(
        key = "make_own_event",
        label = "Провёл своё мероприятие",
        mode = PixelColumnMode.Fixed,
        fixedPoints = 100
    ),
    PixelColumnDef(
        key = "special_achievements",
        label = "Особые достижения",
        mode = PixelColumnMode.Number
    ),
    PixelColumnDef(
        key = "fine",
        label = "Штрафы",
        mode = PixelColumnMode.Penalty
    ),
    PixelColumnDef(
        key = "__attendance_percent__",
        label = "% посещаемости",
        mode = PixelColumnMode.Readonly,
        hint = "При посещении нескольких групп берётся лучший процент."
    ),
    PixelColumnDef(
        key = "__attendance__",
        label = "Баллы за посещаемость",
        mode = PixelColumnMode.Readonly
    ),
    PixelColumnDef(
        key = "__total__",
        label = "Итого",
        mode = PixelColumnMode.ReadonlyTotal
    )
)
