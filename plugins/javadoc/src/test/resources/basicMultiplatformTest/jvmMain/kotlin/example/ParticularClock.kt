package example

import greeteer.Greeter

class ParticularClock(private val clockDay: ClockDays) : Clock() {

    /**
     * Rings bell [times]
     */
    fun ringBell(times: Int) {}

    /**
     * Uses provider [greeter]
     */
    fun useGreeter(greeter: Greeter) {

    }

    /**
     * Day of the week
     */
    override fun getDayOfTheWeek() = clockDay.name
}