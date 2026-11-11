package com.cibotimer.domain

import com.cibotimer.data.models.DayPlan
import com.cibotimer.data.models.FastingPeriod
import com.cibotimer.data.models.FastingPlan
import com.cibotimer.data.models.PlanCategory

object FastingPlanTemplates {

    fun createStandardDays(
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        fastingMinutes: Int
    ): List<DayPlan> {
        return (1..7).map { dayIndex ->
            DayPlan(
                dayOfWeek = dayIndex,
                isFastingDay = true,
                startHour = startHour,
                startMinute = startMinute,
                endHour = endHour,
                endMinute = endMinute,
                fastingDurationMinutes = fastingMinutes
            )
        }
    }

    val PLAN_16_8 = FastingPlan(
        id = "16-8",
        name = "16-8 (Classico)",
        category = PlanCategory.STANDARD,
        description = "Il metodo più amato e collaudato: 16 ore di digiuno e una finestra alimentare di 8 ore (dalle 12:00 alle 20:00). Ideale sia per principianti che per esperti.",
        days = createStandardDays(startHour = 20, startMinute = 0, endHour = 12, endMinute = 0, fastingMinutes = 960)
    )

    val PLAN_16_8_LATE = FastingPlan(
        id = "16-8-late",
        name = "16-8 (Serale / Tardi)",
        category = PlanCategory.STANDARD,
        description = "Pensato per chi va a letto tardi o cena tardi: digiuno dalle 22:00 alle 14:00 del giorno dopo.",
        days = createStandardDays(startHour = 22, startMinute = 0, endHour = 14, endMinute = 0, fastingMinutes = 960)
    )

    val PLAN_14_10 = FastingPlan(
        id = "14-10",
        name = "14-10 (Dolce)",
        category = PlanCategory.STANDARD,
        description = "Perfetto per chi si avvicina per la prima volta al digiuno intermittente. 14 ore di digiuno (20:00 - 10:00) facili da sostenere.",
        days = createStandardDays(startHour = 20, startMinute = 0, endHour = 10, endMinute = 0, fastingMinutes = 840)
    )

    val PLAN_15_9 = FastingPlan(
        id = "15-9",
        name = "15-9 (Intermedio)",
        category = PlanCategory.STANDARD,
        description = "Il gradino perfetto tra il 14-10 e il 16-8 per abituare il corpo in modo progressivo e senza stress.",
        days = createStandardDays(startHour = 20, startMinute = 0, endHour = 11, endMinute = 0, fastingMinutes = 900)
    )

    val PLAN_18_6 = FastingPlan(
        id = "18-6",
        name = "18-6 (Avanzato)",
        category = PlanCategory.AUTOPHAGY,
        description = "18 ore di digiuno al giorno. Attiva pienamente la chetosi e l'inizio dell'autofagia cellulare per una perdita di grasso accelerata.",
        days = createStandardDays(startHour = 20, startMinute = 0, endHour = 14, endMinute = 0, fastingMinutes = 1080)
    )

    val PLAN_20_4 = FastingPlan(
        id = "20-4",
        name = "20-4 (La Dieta del Guerriero)",
        category = PlanCategory.AUTOPHAGY,
        description = "Ispirato agli antichi guerrieri: 20 ore di digiuno con una finestra di alimentazione concentrata in 4 ore serali.",
        days = createStandardDays(startHour = 20, startMinute = 0, endHour = 16, endMinute = 0, fastingMinutes = 1200)
    )

    val PLAN_23_1_OMAD = FastingPlan(
        id = "23-1",
        name = "23-1 (OMAD - One Meal A Day)",
        category = PlanCategory.SPECIAL,
        description = "Un unico pasto nutriente al giorno con 23 ore di digiuno. Massima pulizia cellulare, massima concentrazione ed energia.",
        days = createStandardDays(startHour = 20, startMinute = 0, endHour = 19, endMinute = 0, fastingMinutes = 1380)
    )

    val PLAN_5_2 = FastingPlan(
        id = "5-2",
        name = "5-2 (Due giorni completi)",
        category = PlanCategory.SPECIAL,
        description = "5 giorni di alimentazione normale e 2 giorni (Martedì e Giovedì) con digiuno prolungato di 24 ore.",
        days = listOf(
            DayPlan(1, false, 0, 0, 0, 0, 0),
            DayPlan(2, true, 20, 0, 20, 0, 1440), // 24h
            DayPlan(3, false, 0, 0, 0, 0, 0),
            DayPlan(4, true, 20, 0, 20, 0, 1440), // 24h
            DayPlan(5, false, 0, 0, 0, 0, 0),
            DayPlan(6, false, 0, 0, 0, 0, 0),
            DayPlan(7, false, 0, 0, 0, 0, 0)
        )
    )

    val PLAN_AUTOPHAGY_BOOSTER = FastingPlan(
        id = "autophagy-booster",
        name = "Booster di Autofagia",
        category = PlanCategory.AUTOPHAGY,
        description = "Un piano a giorni alterni alternando 16-8, 18-6 e un picco di 22h per stimolare al massimo il riciclo cellulare e la longevità.",
        days = listOf(
            DayPlan(1, true, 20, 0, 12, 0, 960),  // 16h
            DayPlan(2, true, 20, 0, 14, 0, 1080), // 18h
            DayPlan(3, true, 20, 0, 12, 0, 960),  // 16h
            DayPlan(4, true, 20, 0, 18, 0, 1320), // 22h Booster
            DayPlan(5, true, 20, 0, 12, 0, 960),  // 16h
            DayPlan(6, true, 20, 0, 14, 0, 1080), // 18h
            DayPlan(7, true, 20, 0, 12, 0, 960)   // 16h
        )
    )

    val PLAN_LUIGI_1 = FastingPlan(
        id = "luigi-1",
        name = "Luigi 1",
        category = PlanCategory.SPECIAL,
        description = "Piano Coach CiboTimer con 9 periodi ad alta intensità, finestre pasto calibrate e recupero fisiologico nel weekend.",
        days = listOf(
            DayPlan(dayOfWeek = 1, isFastingDay = true, startHour = 14, startMinute = 0, endHour = 10, endMinute = 0, fastingDurationMinutes = 1200), // Lun: 14:00 -> Mar 10:00 (20h)
            DayPlan(dayOfWeek = 2, isFastingDay = true, startHour = 11, startMinute = 0, endHour = 7, endMinute = 0, fastingDurationMinutes = 1200),  // Mar: 11:00 -> Mer 07:00 (20h)
            DayPlan(dayOfWeek = 3, isFastingDay = true, startHour = 12, startMinute = 0, endHour = 10, endMinute = 0, fastingDurationMinutes = 1320), // Mer: 12:00 -> Gio 10:00 (22h)
            DayPlan(dayOfWeek = 4, isFastingDay = true, startHour = 12, startMinute = 0, endHour = 11, endMinute = 0, fastingDurationMinutes = 1380), // Gio: 12:00 -> Ven 11:00 (23h)
            DayPlan(dayOfWeek = 5, isFastingDay = true, startHour = 12, startMinute = 0, endHour = 8, endMinute = 0, fastingDurationMinutes = 1200),  // Ven: 12:00 -> Sab 08:00 (20h)
            DayPlan(dayOfWeek = 6, isFastingDay = true, startHour = 21, startMinute = 0, endHour = 10, endMinute = 0, fastingDurationMinutes = 780),   // Sab: 21:00 -> Dom 10:00 (13h)
            DayPlan(dayOfWeek = 7, isFastingDay = true, startHour = 16, startMinute = 0, endHour = 11, endMinute = 0, fastingDurationMinutes = 1140)  // Dom: 16:00 -> Lun 11:00 (19h)
        ),
        periods = listOf(
            FastingPeriod(id = "p1", periodNumber = 1, startDayOfWeek = 1, startHour = 0, startMinute = 0, endDayOfWeek = 1, endHour = 13, endMinute = 0, fastingDurationMinutes = 780),   // Periodo 1: lun 00:00 – lun 13:00 (13h)
            FastingPeriod(id = "p2", periodNumber = 2, startDayOfWeek = 1, startHour = 14, startMinute = 0, endDayOfWeek = 2, endHour = 10, endMinute = 0, fastingDurationMinutes = 1200), // Periodo 2: lun 14:00 – mar 10:00 (20h)
            FastingPeriod(id = "p3", periodNumber = 3, startDayOfWeek = 2, startHour = 11, startMinute = 0, endDayOfWeek = 3, endHour = 7, endMinute = 0, fastingDurationMinutes = 1200),  // Periodo 3: mar 11:00 – mer 07:00 (20h)
            FastingPeriod(id = "p4", periodNumber = 4, startDayOfWeek = 3, startHour = 12, startMinute = 0, endDayOfWeek = 4, endHour = 10, endMinute = 0, fastingDurationMinutes = 1320), // Periodo 4: mer 12:00 – gio 10:00 (22h)
            FastingPeriod(id = "p5", periodNumber = 5, startDayOfWeek = 4, startHour = 12, startMinute = 0, endDayOfWeek = 5, endHour = 11, endMinute = 0, fastingDurationMinutes = 1380), // Periodo 5: gio 12:00 – ven 11:00 (23h)
            FastingPeriod(id = "p6", periodNumber = 6, startDayOfWeek = 5, startHour = 12, startMinute = 0, endDayOfWeek = 6, endHour = 8, endMinute = 0, fastingDurationMinutes = 1200),  // Periodo 6: ven 12:00 – sab 08:00 (20h)
            FastingPeriod(id = "p7", periodNumber = 7, startDayOfWeek = 6, startHour = 21, startMinute = 0, endDayOfWeek = 7, endHour = 10, endMinute = 0, fastingDurationMinutes = 780),   // Periodo 7: sab 21:00 – dom 10:00 (13h)
            FastingPeriod(id = "p8", periodNumber = 8, startDayOfWeek = 7, startHour = 16, startMinute = 0, endDayOfWeek = 8, endHour = 11, endMinute = 0, fastingDurationMinutes = 1140),  // Periodo 8: dom 16:00 – lun 11:00 (19h)
            FastingPeriod(id = "p9", periodNumber = 9, startDayOfWeek = 8, startHour = 11, startMinute = 0, endDayOfWeek = 8, endHour = 23, endMinute = 0, fastingDurationMinutes = 720)   // Periodo 9: lun 11:00 – lun 23:00 (12h)
        )
    )

    val ALL_PREDEFINED_PLANS = listOf(
        PLAN_LUIGI_1,
        PLAN_16_8,
        PLAN_16_8_LATE,
        PLAN_14_10,
        PLAN_15_9,
        PLAN_18_6,
        PLAN_20_4,
        PLAN_23_1_OMAD,
        PLAN_5_2,
        PLAN_AUTOPHAGY_BOOSTER
    )

    fun getById(id: String): FastingPlan {
        return ALL_PREDEFINED_PLANS.firstOrNull { it.id == id } ?: PLAN_16_8
    }
}
