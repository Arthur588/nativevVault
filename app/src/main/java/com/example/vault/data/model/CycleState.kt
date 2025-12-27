package com.example.vault.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cycle_state")
/**
 * CycleState keeps track of the current randomised order of media IDs and the
 * position within that order.  A single row with id=0 is stored in the
 * cycle_state table.  Fields:
 *
 * - orderList: Comma separated list of all media IDs in a random order.  When
 *   the cycle completes (pointer reaches the end), a new random order list is
 *   generated.
 * - pointer: Index of the first item in [orderList] that has not yet been
 *   permanently consumed in this cycle.  When a day boundary passes and
 *   [dailyIndex] > 0, [pointer] is incremented by [dailyIndex] to remove
 *   consumed items from the cycle.
 * - dailyIndex: Number of items from the current day that have been viewed.
 *   Each day up to seven items are selected starting at [pointer].  As the
 *   user views each item, [dailyIndex] increments.  When the day boundary
 *   passes and [dailyIndex] > 0, [pointer] is advanced by [dailyIndex] and
 *   [dailyIndex] resets to 0.  If the user does not open the app on a given
 *   day and [dailyIndex] == 0, the same set of seven items (or fewer if
 *   remaining) will be offered on the next day until they are consumed.
 * - currentDayStart: Timestamp (milliseconds since epoch) representing the
 *   start of the current day at 07:00 local time.  Each time the app
 *   computes the list for "today" it compares this timestamp against the
 *   calculated start of the current day and updates [pointer], [dailyIndex]
 *   and [currentDayStart] accordingly.
 */
data class CycleState(
    @PrimaryKey val id: Int = 0,
    val orderList: String,
    val pointer: Int,
    val dailyIndex: Int,
    val currentDayStart: Long
)