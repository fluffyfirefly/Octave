/*
 * MIT License
 *
 * Copyright (c) 2020 Melms Media LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

//Original class in Mantaro made by natanbc
package gg.octave.bot.entities.sharding

import gg.octave.bot.Launcher
import net.dv8tion.jda.api.utils.SessionController
import net.dv8tion.jda.api.utils.SessionController.SessionConnectNode
import net.dv8tion.jda.api.utils.SessionControllerAdapter
import javax.annotation.CheckReturnValue
import javax.annotation.Nonnegative
import javax.annotation.Nonnull

class BucketedController(@Nonnegative bucketFactor: Int, homeGuildId: Long) : SessionControllerAdapter() {
    private val shardControllers: Array<SessionController?>

    init {
        require(bucketFactor >= 1) { "Bucket factor must be at least 1" }
        shardControllers = arrayOfNulls(bucketFactor)
        for (i in 0 until bucketFactor) {
            shardControllers[i] = PrioritizingSessionController(homeGuildId)
        }
    }

    @Nonnull
    @CheckReturnValue
    private fun controllerFor(@Nonnull node: SessionConnectNode): SessionController? {
        return shardControllers[node.shardInfo.shardId % shardControllers.size]
    }

    override fun appendSession(@Nonnull node: SessionConnectNode) {
        controllerFor(node)!!.appendSession(node)
    }

    override fun removeSession(@Nonnull node: SessionConnectNode) {
        controllerFor(node)!!.removeSession(node)
    }

//    override fun getGlobalRatelimit(): Long = Launcher.database.getGlobalRatelimit()
//    override fun setGlobalRatelimit(ratelimit: Long) = Launcher.database.setGlobalRatelimit(ratelimit)

}