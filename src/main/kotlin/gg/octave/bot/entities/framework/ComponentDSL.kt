//package gg.octave.bot.entities.framework
//
//import net.dv8tion.jda.api.events.GenericEvent
//import net.dv8tion.jda.api.events.interaction.ButtonClickEvent
//import net.dv8tion.jda.api.hooks.EventListener
//import net.dv8tion.jda.api.interactions.components.ActionRow
//import net.dv8tion.jda.api.interactions.components.Button
//import java.util.concurrent.TimeUnit
//
//object ComponentDSL : EventListener {
//    //private val forms:
//    override fun onEvent(event: GenericEvent) {
//        //if (event is ButtonClickEvent) {
//
//        //}
//    }
//}
//
//class ComponentForm
//
//class FormBuilder {
//    private val components = mutableListOf<ActionRow>()
//    private val timeout = -1
//
//    inner class EventPredicate {
//        // By default, this will store all given predicates in an array. All predicates
//        // are chained together in an "AND" manner, so they will all need to return true
//        // for event processing to proceed. If "OR" chaining, or more granular control is
//        // needed over filtering, "raw" should be used.
//        private val predicates = mutableListOf<(ButtonClickEvent) -> Boolean>()
//
//        fun raw(pred: (ButtonClickEvent) -> Boolean) = predicates.add(pred)
//
//        fun user(id: Long) = predicates.add { it.user.idLong == id }
//
//        fun channel(id: Long) = predicates.add { it.channel.idLong == id }
//
//        fun message(id: Long) = predicates.add { it.messageIdLong == id }
//
//        fun test(event: ButtonClickEvent): Boolean = predicates.all { it(event) }
//    }
//
//    inner class RowBuilder {
//        //private val components = mutableListOf<>()
//
//        fun button(id: String, applicator: Button.() -> Unit) {
//
//        }
//
//        // Custom Properties
//
//        // -1/Omit = unlimited, 0 = disabled, 1 = single use.
//        fun Button.maxUses(uses: Int) {
//
//        }
//
//        fun Button.timeout(delay: Int, unit: TimeUnit) {
//
//        }
//
//        fun Button.predicate(predicate: EventPredicate.() -> Unit) {
//
//        }
//
//        // Events
//
//        /**
//         * A callback that's fired when the button receives a click event
//         */
//        fun Button.onClick(event: (ButtonClickEvent) -> Unit) {
//
//        }
//
//        /**
//         * A callback that's fired when the timeout for the button elapses.
//         * This will never fire if a timeout is not set.
//         */
//        fun Button.onTimeoutElapsed(event: () -> Unit) {
//
//        }
//
//        /**
//         * A callback that's fired when the remaining uses of the button reaches zero.
//         * This will never fire if the max uses is unset/unlimited, or if max uses started at zero (disabled).
//         */
//        fun Button.onNoMoreUses(event: () -> Unit) {
//
//        }
//
//        internal fun build(): ActionRow {
//
//        }
//    }
//
//    fun row(applicator: RowBuilder.() -> Unit) {
//        //RowBuilder().apply(applicator).build()
//    }
//
//    /**
//     * Sets a form-wide timeout. This is highly advised otherwise forms
//     * will build up in memory.
//     */
//    fun timeout(delay: Int, unit: TimeUnit) {
//
//    }
//
//    fun build(): ComponentForm {
//
//    }
//}
//
//fun buildForm(builder: FormBuilder.() -> Unit) {
//
//}
