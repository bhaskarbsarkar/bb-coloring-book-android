package com.bb.coloringbook

/** Original, simple cartoon pages for kids (100x100 units). Shapes drawn later cover earlier ones. */
object KidsArt {
    val all: List<Pair<String, (Pen) -> Unit>> = listOf(
        "Kitty" to { p -> p.run {
            line(0f, 94f, 100f, 94f)
            path(whiteFill = true) { m(68f, 80f); c(88f, 80f, 92f, 58f, 84f, 46f); l(79f, 49f); c(85f, 60f, 82f, 72f, 68f, 73f); z() }
            ovalDisc(28f, 50f, 72f, 92f)
            oval(40f, 60f, 60f, 86f)
            ovalDisc(33f, 86f, 46f, 94f); ovalDisc(54f, 86f, 67f, 94f)
            path(whiteFill = true) { m(33f, 30f); l(31f, 8f); l(47f, 21f); z() }
            path(whiteFill = true) { m(67f, 30f); l(69f, 8f); l(53f, 21f); z() }
            disc(50f, 36f, 18f)
            path { m(35f, 22f); l(34f, 13f); l(41f, 19f) }
            path { m(65f, 22f); l(66f, 13f); l(59f, 19f) }
            ovalDisc(40f, 29f, 47f, 38f); dot(44f, 34f, 1.8f)
            ovalDisc(53f, 29f, 60f, 38f); dot(56.5f, 34f, 1.8f)
            path(whiteFill = true) { m(47.5f, 40f); l(52.5f, 40f); l(50f, 43f); z() }
            path { m(50f, 43f); q(48f, 47f, 45f, 45f) }; path { m(50f, 43f); q(52f, 47f, 55f, 45f) }
            line(38f, 42f, 25f, 40f); line(38f, 45f, 25f, 47f); line(62f, 42f, 75f, 40f); line(62f, 45f, 75f, 47f)
        } },
        "Puppy" to { p -> p.run {
            ovalDisc(28f, 20f, 72f, 74f)
            oval(56f, 24f, 68f, 38f)
            path(whiteFill = true) { m(33f, 26f); c(18f, 22f, 10f, 52f, 19f, 63f); c(26f, 68f, 33f, 56f, 33f, 42f); z() }
            path(whiteFill = true) { m(67f, 26f); c(82f, 22f, 90f, 52f, 81f, 63f); c(74f, 68f, 67f, 56f, 67f, 42f); z() }
            ovalDisc(35f, 46f, 65f, 74f)
            path(whiteFill = true) { m(45.5f, 62f); c(45.5f, 73f, 54.5f, 73f, 54.5f, 62f); z() }
            line(50f, 62f, 50f, 68f)
            line(50f, 55f, 50f, 61f)
            path { m(50f, 61f); q(45f, 66f, 40f, 61f) }; path { m(50f, 61f); q(55f, 66f, 60f, 61f) }
            dot(50f, 52f, 3.6f)
            disc(41f, 39f, 4.5f); dot(42f, 40f, 2.2f)
            disc(59f, 39f, 4.5f); dot(60f, 40f, 2.2f)
            rectDisc(33f, 74f, 67f, 82f, 3f)
            disc(50f, 87f, 5f); circle(50f, 87f, 2f)
        } },
        "Fish" to { p -> p.run {
            path(whiteFill = true) { m(68f, 50f); l(90f, 32f); q(84f, 50f, 90f, 68f); z() }
            path(whiteFill = true) { m(34f, 34f); q(42f, 14f, 62f, 34f); z() }
            path(whiteFill = true) { m(40f, 66f); q(46f, 80f, 58f, 66f); z() }
            path(whiteFill = true) { m(12f, 50f); c(24f, 24f, 62f, 24f, 74f, 50f); c(62f, 76f, 24f, 76f, 12f, 50f); z() }
            clip({ m(12f, 50f); c(24f, 24f, 62f, 24f, 74f, 50f); c(62f, 76f, 24f, 76f, 12f, 50f); z() }) {
                for (x in listOf(38f, 48f, 58f)) path { m(x, 20f); q(x + 6f, 50f, x, 80f) }
            }
            path { m(28f, 36f); q(36f, 50f, 28f, 64f) }
            disc(21f, 46f, 4f); dot(22f, 46f, 2f)
            path { m(12f, 50f); q(15f, 53f, 18f, 52f) }
            circle(86f, 20f, 3f); circle(80f, 11f, 2.2f); circle(91f, 8f, 1.6f)
            path(whiteFill = true) { m(8f, 100f); q(2f, 86f, 10f, 76f); q(16f, 88f, 13f, 100f); z() }
            path(whiteFill = true) { m(16f, 100f); q(12f, 90f, 20f, 84f); q(24f, 92f, 21f, 100f); z() }
            path(whiteFill = true) { m(84f, 100f); q(80f, 84f, 90f, 74f); q(94f, 88f, 90f, 100f); z() }
        } },
        "Owl" to { p -> p.run {
            circle(84f, 14f, 8f)
            path(whiteFill = true) { m(30f, 42f); l(27f, 20f); l(43f, 32f); z() }
            path(whiteFill = true) { m(70f, 42f); l(73f, 20f); l(57f, 32f); z() }
            ovalDisc(27f, 28f, 73f, 86f)
            oval(37f, 52f, 63f, 84f)
            for ((x, y) in listOf(44f to 62f, 50f to 62f, 56f to 62f, 47f to 70f, 53f to 70f, 50f to 77f))
                path { m(x - 2.5f, y); q(x, y + 3.5f, x + 2.5f, y) }
            path(whiteFill = true) { m(29f, 50f); c(19f, 62f, 23f, 78f, 36f, 84f); c(33f, 72f, 33f, 60f, 29f, 50f); z() }
            path(whiteFill = true) { m(71f, 50f); c(81f, 62f, 77f, 78f, 64f, 84f); c(67f, 72f, 67f, 60f, 71f, 50f); z() }
            disc(40f, 42f, 8.5f); disc(40f, 42f, 4.5f); dot(41f, 42f, 2.2f)
            disc(60f, 42f, 8.5f); disc(60f, 42f, 4.5f); dot(61f, 42f, 2.2f)
            path(whiteFill = true) { m(47f, 49f); l(53f, 49f); l(50f, 56f); z() }
            rectDisc(6f, 84f, 94f, 91f, 3f)
            for (x in listOf(41f, 46f, 54f, 59f)) ovalDisc(x - 2.5f, 81f, x + 2.5f, 87f)
            path(whiteFill = true) { m(80f, 91f); q(86f, 96f, 92f, 94f); q(86f, 92f, 84f, 91f); z() }
        } },
        "Butterfly" to { p -> p.run {
            for (sx in listOf(1f, -1f)) {
                fun x(v: Float) = 50f + sx * (v - 50f)
                path(whiteFill = true) { m(50f, 46f); c(x(30f), 8f, x(4f), 12f, x(9f), 34f); c(x(12f), 50f, x(30f), 52f, 50f, 50f); z() }
                path(whiteFill = true) { m(50f, 52f); c(x(30f), 54f, x(12f), 64f, x(20f), 82f); c(x(28f), 94f, x(46f), 78f, 50f, 58f); z() }
                circle(x(28f), 31f, 8f); circle(x(28f), 31f, 3.5f); circle(x(14f), 34f, 2.5f)
                circle(x(32f), 70f, 6f); circle(x(23f), 80f, 2f)
            }
            ovalDisc(46f, 34f, 54f, 82f)
            line(46.5f, 50f, 53.5f, 50f); line(46.5f, 60f, 53.5f, 60f); line(47f, 70f, 53f, 70f)
            disc(50f, 29f, 5.5f)
            path { m(48f, 24f); q(42f, 12f, 36f, 10f) }; path { m(52f, 24f); q(58f, 12f, 64f, 10f) }
            disc(36f, 10f, 2f); disc(64f, 10f, 2f)
        } },
        "Cozy house" to { p -> p.run {
            circle(14f, 14f, 7f)
            for (i in 0 until 8) {
                val a = i * Math.PI / 4
                line(14f + (Math.cos(a) * 9.5).toFloat(), 14f + (Math.sin(a) * 9.5).toFloat(), 14f + (Math.cos(a) * 13).toFloat(), 14f + (Math.sin(a) * 13).toFloat())
            }
            disc(62f, 14f, 5f); disc(70f, 11f, 6.5f); disc(78f, 14f, 5f); line(62f, 19f, 78f, 19f)
            line(0f, 86f, 100f, 86f)
            rectDisc(52f, 28f, 58f, 44f)
            rectDisc(22f, 50f, 62f, 86f)
            path(whiteFill = true) { m(16f, 52f); l(42f, 28f); l(68f, 52f); z() }
            circle(42f, 42f, 3.5f)
            rect(36f, 64f, 48f, 86f, 2f); dot(46f, 76f, 0.9f)
            rect(25f, 58f, 33f, 67f); line(29f, 58f, 29f, 67f); line(25f, 62.5f, 33f, 62.5f)
            rect(51f, 58f, 59f, 67f); line(55f, 58f, 55f, 67f); line(51f, 62.5f, 59f, 62.5f)
            path { m(38f, 86f); q(36f, 94f, 28f, 100f) }; path { m(46f, 86f); q(50f, 94f, 58f, 100f) }
            rectDisc(76f, 62f, 82f, 86f)
            disc(72f, 56f, 7f); disc(86f, 56f, 7f); disc(79f, 48f, 10f)
            for ((x, y) in listOf(10f to 94f, 90f to 94f, 70f to 96f)) { disc(x, y, 2f); dot(x, y, 0.7f) }
        } },
        "Rocket" to { p -> p.run {
            circle(82f, 22f, 7f); oval(71f, 19.5f, 93f, 24.5f)
            for ((x, y) in listOf(16f to 18f, 24f to 46f, 84f to 56f, 12f to 72f, 88f to 86f)) path {
                m(x, y - 4f); q(x, y, x + 4f, y); q(x, y, x, y + 4f); q(x, y, x - 4f, y); q(x, y, x, y - 4f); z()
            }
            path(whiteFill = true) { m(42f, 73f); q(50f, 98f, 58f, 73f); z() }
            path { m(46f, 74f); q(50f, 88f, 54f, 74f) }
            path(whiteFill = true) { m(39f, 56f); l(27f, 78f); l(41f, 72f); z() }
            path(whiteFill = true) { m(61f, 56f); l(73f, 78f); l(59f, 72f); z() }
            path(whiteFill = true) { m(50f, 8f); c(65f, 20f, 65f, 52f, 62f, 74f); l(38f, 74f); c(35f, 52f, 35f, 20f, 50f, 8f); z() }
            clip({ m(50f, 8f); c(65f, 20f, 65f, 52f, 62f, 74f); l(38f, 74f); c(35f, 52f, 35f, 20f, 50f, 8f); z() }) {
                path { m(30f, 26f); q(50f, 22f, 70f, 26f) }
                line(30f, 64f, 70f, 64f)
            }
            disc(50f, 41f, 7.5f); circle(50f, 41f, 4.5f)
            line(50f, 64f, 50f, 74f)
        } },
        "Car" to { p -> p.run {
            disc(20f, 14f, 5f); disc(28f, 11f, 6.5f); disc(36f, 14f, 5f); line(20f, 19f, 36f, 19f)
            line(0f, 77f, 100f, 77f)
            path(whiteFill = true) {
                m(8f, 66f); l(8f, 54f); q(10f, 47f, 20f, 46f); l(30f, 46f); l(40f, 32f); l(66f, 32f); l(78f, 46f)
                l(88f, 48f); q(94f, 50f, 94f, 58f); l(94f, 66f); z()
            }
            path { m(34f, 46f); l(42f, 36f); l(52f, 36f); l(52f, 46f); z() }
            path { m(56f, 46f); l(56f, 36f); l(64f, 36f); l(72f, 46f); z() }
            line(54f, 46f, 54f, 66f); line(58f, 52f, 63f, 52f)
            line(8f, 58f, 94f, 58f)
            oval(87f, 50f, 93f, 55f); rect(8f, 49f, 13f, 54f, 1f)
            disc(26f, 68f, 9f); circle(26f, 68f, 4f)
            disc(76f, 68f, 9f); circle(76f, 68f, 4f)
            for (x in listOf(10f, 40f, 70f)) line(x, 88f, x + 14f, 88f)
        } },
        "Turtle" to { p -> p.run {
            line(0f, 80f, 100f, 80f)
            ovalDisc(22f, 62f, 34f, 80f); ovalDisc(62f, 62f, 74f, 80f)
            path(whiteFill = true) { m(14f, 64f); l(6f, 67f); l(14f, 70f); z() }
            path(whiteFill = true) { m(80f, 64f); q(78f, 54f, 84f, 50f); l(88f, 62f); z() }
            disc(86f, 52f, 9f); dot(89f, 49f, 1.6f)
            path { m(84f, 56f); q(88f, 59f, 92f, 56f) }
            path(whiteFill = true) { m(14f, 66f); c(14f, 26f, 84f, 26f, 84f, 66f); z() }
            clip({ m(14f, 66f); c(14f, 26f, 84f, 26f, 84f, 66f); z() }) {
                val hx = listOf(49f to 48f, 33f to 56f, 65f to 56f, 33f to 40f, 65f to 40f, 49f to 32f)
                for ((x, y) in hx) path {
                    for (i in 0..6) {
                        val a = Math.PI / 3 * i
                        val px = x + (Math.cos(a) * 9).toFloat(); val py = y + (Math.sin(a) * 9).toFloat()
                        if (i == 0) m(px, py) else l(px, py)
                    }
                    z()
                }
            }
            rectDisc(11f, 63f, 87f, 69f, 3f)
        } },
        "Elephant" to { p -> p.run {
            line(0f, 92f, 100f, 92f)
            rectDisc(44f, 68f, 54f, 92f, 3f); rectDisc(76f, 68f, 86f, 92f, 3f)
            line(90f, 56f, 95f, 68f); path(whiteFill = true) { m(95f, 68f); l(93f, 73f); l(98f, 72f); z() }
            ovalDisc(38f, 38f, 92f, 80f)
            rectDisc(58f, 70f, 68f, 92f, 3f)
            path(whiteFill = true) { m(22f, 46f); c(16f, 60f, 10f, 74f, 16f, 84f); l(23f, 83f); c(18f, 72f, 24f, 60f, 32f, 54f); z() }
            disc(34f, 40f, 16f)
            path(whiteFill = true) { m(38f, 28f); c(60f, 18f, 64f, 52f, 46f, 58f); c(42f, 56f, 40f, 46f, 38f, 28f); z() }
            dot(28f, 37f, 1.8f)
            path { m(24f, 46f); q(28f, 49f, 31f, 46f) }
            for (x in listOf(46f, 50f, 60f, 64f, 78f, 82f)) path { m(x - 1.5f, 92f); q(x, 89f, x + 1.5f, 92f) }
        } },
        "Ice cream" to { p -> p.run {
            path(whiteFill = true) { m(34f, 52f); l(50f, 96f); l(66f, 52f); z() }
            clip({ m(34f, 52f); l(50f, 96f); l(66f, 52f); z() }) {
                for (i in -3..3) { line(40f + i * 7f, 50f, 60f + i * 7f, 100f); line(60f + i * 7f, 50f, 40f + i * 7f, 100f) }
            }
            disc(50f, 30f, 13f)
            for ((x, y, d) in listOf(Triple(44f, 24f, 1f), Triple(52f, 21f, -1f), Triple(56f, 30f, 1f), Triple(45f, 33f, -1f)))
                line(x, y, x + 3f, y + 2f * d)
            path(whiteFill = true) {
                m(29f, 54f); q(28f, 38f, 50f, 37f); q(72f, 38f, 71f, 54f)
                q(68f, 62f, 64f, 54f); q(60f, 64f, 56f, 54f); q(51f, 61f, 46f, 54f); q(41f, 64f, 37f, 54f); q(33f, 62f, 29f, 54f); z()
            }
            path { m(52f, 14f); q(54f, 8f, 60f, 6f) }
            disc(50f, 14f, 4.5f)
        } },
        "Dino" to { p -> p.run {
            line(0f, 90f, 100f, 90f)
            for ((x, y) in listOf(34f to 50f, 42f to 46f, 50f to 45f, 58f to 46f, 66f to 49f))
                path(whiteFill = true) { m(x - 4f, y + 4f); l(x, y - 6f); l(x + 4f, y + 4f); z() }
            path(whiteFill = true) { m(32f, 62f); q(16f, 66f, 4f, 80f); q(22f, 80f, 36f, 72f); z() }
            rectDisc(34f, 70f, 45f, 90f, 3f); rectDisc(60f, 70f, 71f, 90f, 3f)
            ovalDisc(28f, 46f, 78f, 82f)
            oval(38f, 62f, 66f, 80f)
            path(whiteFill = true) { m(62f, 58f); q(64f, 40f, 68f, 26f); l(80f, 26f); q(78f, 44f, 77f, 62f); z() }
            ovalDisc(62f, 14f, 94f, 34f)
            dot(83f, 21f, 1.6f)
            path { m(80f, 28f); q(86f, 31f, 91f, 27f) }
            circle(46f, 56f, 2.5f); circle(54f, 52f, 2f); circle(40f, 60f, 1.8f)
        } },
    )
}
