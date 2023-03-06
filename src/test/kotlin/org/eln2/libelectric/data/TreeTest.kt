package org.eln2.libelectric.data

import org.ageseries.libage.data.BinaryTree
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class TreeTest {
    object TestTrees {
        fun empty() = BinaryTree<Int>(null)
        fun one() = BinaryTree(0)
        fun complete(levels: Int): BinaryTree<Int> {
            if(levels == 0) return empty()
            val tree = BinaryTree(0)
            repeat(levels - 1) {
                tree.root!!.filter { it.isLeaf }.toList().forEach {
                    it.insert(BinaryTree.Direction.LEFT, 2*it.value + 1)
                    it.insert(BinaryTree.Direction.RIGHT, 2*it.value + 2)
                }
            }
            return tree
        }
        fun path(levels: Int, direction: BinaryTree.Direction): BinaryTree<Int> {
            if(levels == 0) return empty()
            val tree = BinaryTree(0)
            var cur = tree.root!!
            repeat(levels - 1) {
                cur = cur.insert(direction, it + 1)
            }
            return tree
        }
        fun zig(levels: Int, first: BinaryTree.Direction): BinaryTree<Int> {
            if(levels == 0) return empty()
            val tree = BinaryTree(0)
            var dir = first
            var cur = tree.root!!
            repeat(levels - 1) {
                cur = cur.insert(dir, it + 1)
                dir = dir.opposite
            }
            return tree
        }
        val ALL_LIM = 5
        fun all(): List<BinaryTree<Int>> = buildList {
            add(empty())
            add(one())
            repeat(ALL_LIM) { add(complete(it + 2)) }
            repeat(ALL_LIM) { add(path(it + 2, BinaryTree.Direction.LEFT)) }
            repeat(ALL_LIM) { add(path(it + 2, BinaryTree.Direction.RIGHT)) }
            repeat(ALL_LIM) { add(zig(it + 2, BinaryTree.Direction.LEFT)) }
            repeat(ALL_LIM) { add(zig(it + 2, BinaryTree.Direction.RIGHT)) }
        }
    }

    fun checkLinks(tree: BinaryTree<Int>) {
        tree.root?.traverse {
            it.left?.apply { assertEquals(parent, it) { "left child did not have this as parent" } }
            it.right?.apply { assertEquals(parent, it) { "right child did not have this as parent" } }
        }
    }

    @Test
    fun consistent() {
        TestTrees.all().forEach {
            checkLinks(it)
        }
    }

    @Test
    fun sorted() {
        repeat(7) {
            var last: Int? = null
            TestTrees.complete(it).root?.traverse(BinaryTree.Traversal.INORDER) {
                last?.apply { assert(this < it.value) { "Out of order in INORDER" } }
                last = it.value
            }
        }
    }
}