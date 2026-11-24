package uk.sugar.lock

import java.time.LocalDate

/** A little diabetes joke on the widget, a new one each day. */
object Jokes {
    private val list = listOf(
        "My pancreas called in sick. Permanently.",
        "I'm not sweet. My blood is.",
        "Carb counting: the only maths I do before breakfast.",
        "Sensor says I'm high. Sadly not the fun kind.",
        "Me and my pump? We're very attached.",
        "I don't have a sweet tooth. I have a sweet bloodstream.",
        "My CGM knows me better than my friends do.",
        "Juice box: snack, medicine and lifestyle.",
        "\"Can you eat that?\" Yes. Next question.",
        "3am hypo: nature's least-wanted alarm clock.",
        "I count carbs so you don't have to.",
        "My algorithm works nights so I can sleep.",
        "Pizza: the final boss of bolusing.",
        "Sugar-free? I'm already doing my part.",
        "My blood type is glucose-positive.",
        "My pancreas has been outsourced to Cambridge.",
        "Low battery and low sugar at once: real horror.",
        "Basically a cyborg with a snack habit.",
        "Exercise: guessing where my sugar goes next.",
        "Glucose tabs: the only chalk I'm happy to eat.",
        "My phone beeps, my pump beeps, I beep.",
        "Me and the algorithm: it's serious.",
        "A flat line on the graph is good news here.",
        "Spare sensor, spare cannula, no spare room.",
        "Dawn phenomenon: my liver's breakfast in bed.",
        "Who needs a fortune teller? I have a trend arrow.",
        "My handbag is 40% jelly babies.",
        "Day off from diabetes? Still pending.",
        "I'm not hangry, I'm just 3.8.",
        "Some people check the weather. I check my graph.",
        "Every number is just information. Some shout louder.",
    )

    fun today(): String = list[LocalDate.now().dayOfYear % list.size]
}
