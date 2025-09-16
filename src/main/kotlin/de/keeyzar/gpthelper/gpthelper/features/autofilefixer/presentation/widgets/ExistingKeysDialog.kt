package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.presentation.widgets

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.rows
import com.intellij.util.ui.tree.TreeUtil
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.dependencyinjection.FlutterArbTranslationInitializer
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*
import javax.swing.text.DefaultHighlighter
import javax.swing.text.Highlighter
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class ExistingKeysDialog(
    private val project: Project,
    private val existingKeys: Map<PsiElement, String>
) : DialogWrapper(project) {

    private lateinit var contextTextArea: JBTextArea
    private lateinit var tree: CheckboxTree
    private val rootNode = CheckedTreeNode(project.name)

    init {
        title = "Select Keys to Replace"
        setOKButtonText("Replace Selected")
        buildTree(existingKeys)
        super.init()
    }

    override fun createCenterPanel(): JComponent {
        val leftPanel = createTreePanel()
        val rightPanel = createContextPanel()

        tree.addTreeSelectionListener {
            val selectedNode = tree.lastSelectedPathComponent as? CheckedTreeNode
            val userObject = selectedNode?.userObject
            if (userObject is PsiElement && userObject.isValid) { // Check if it's a literal
                updateContextView(userObject)
            }
        }

        TreeUtil.expandAll(tree)
        selectFirstFileNode()

        return JBSplitter(false, 0.4f).apply {
            firstComponent = leftPanel
            secondComponent = rightPanel
            preferredSize = Dimension(850, 600)
        }
    }

    private fun createTreePanel(): JComponent {
        tree = CheckboxTree(MyTreeCellRenderer(existingKeys), rootNode)
        tree.model = DefaultTreeModel(rootNode)

        val expandAllButton = JButton("Expand All")
        expandAllButton.addActionListener { TreeUtil.expandAll(tree) }

        val collapseAllButton = JButton("Collapse All")
        collapseAllButton.addActionListener { TreeUtil.collapseAll(tree, 1) }

        val buttonPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(expandAllButton)
            add(collapseAllButton)
            add(Box.createHorizontalGlue())
        }

        return JPanel(BorderLayout()).apply {
            add(buttonPanel, BorderLayout.NORTH)
            add(JBScrollPane(tree), BorderLayout.CENTER)
        }
    }

    private fun createContextPanel(): DialogPanel {
        return panel {
            group("Context") {
                row {
                    textArea()
                        .align(Align.FILL)
                        .rows(20)
                        .also { contextTextArea = it.component }
                        .applyToComponent {
                            isEditable = false
                            isFocusable = false
                            lineWrap = true
                            wrapStyleWord = true
                        }
                }.resizableRow()
            }
        }
    }

    private fun buildTree(stringLiteralsWithSelection: Map<PsiElement, String>) {
        val fileToLiteralsMap = stringLiteralsWithSelection.keys.groupBy { it.containingFile }
        val nodeMap = mutableMapOf<String, CheckedTreeNode>()

        for ((psiFile, literals) in fileToLiteralsMap) {
            if (psiFile == null) continue

            val relativePath = psiFile.virtualFile.path.substringAfter(project.basePath!! + "/")
            val pathComponents = relativePath.split('/')

            var currentParentNode = rootNode
            var currentPath = ""

            for (i in 0 until pathComponents.size - 1) {
                val part = pathComponents[i]
                currentPath += "/$part"
                var childNode = nodeMap[currentPath]
                if (childNode == null) {
                    childNode = CheckedTreeNode(part)
                    nodeMap[currentPath] = childNode
                    currentParentNode.add(childNode)
                }
                currentParentNode = childNode
            }

            val fileNode = CheckedTreeNode(psiFile)
            currentParentNode.add(fileNode)

            for (literal in literals) {
                val literalNode = CheckedTreeNode(literal)
                literalNode.isChecked = true // Select by default
                fileNode.add(literalNode)
            }
        }
    }

    private fun selectFirstFileNode() {
        val firstFileNode = TreeUtil.findNode(rootNode) {
            it.userObject is PsiFile
        }
        if (firstFileNode != null) {
            val path = TreePath((firstFileNode as CheckedTreeNode).path)
            tree.selectionPath = path
            tree.scrollPathToVisible(path)
        }
    }

    private fun updateContextView(element: PsiElement) {
        val contextFinder = FlutterArbTranslationInitializer.create(element.project).literalInContextFinder
        val fullContextText = contextFinder.findContext(element).text
        contextTextArea.text = fullContextText

        contextTextArea.highlighter.removeAllHighlights()

        val stringToHighlight = element.text
        val startIndex = fullContextText.indexOf(stringToHighlight)

        if (startIndex != -1) {
            val endIndex = startIndex + stringToHighlight.length
            try {
                val highlightPainter: Highlighter.HighlightPainter = DefaultHighlighter.DefaultHighlightPainter(JBColor.yellow.darker().darker())
                contextTextArea.highlighter.addHighlight(startIndex, endIndex, highlightPainter)
            } catch (e: Exception) {
                println("Error highlighting text: ${e.message}")
            }
        }
    }

    fun getSelectedElements(): Map<PsiElement, Boolean> {
        val selectedElements = mutableMapOf<PsiElement, Boolean>()
        collectSelectedElements(rootNode, selectedElements)
        return selectedElements
    }

    private fun collectSelectedElements(node: CheckedTreeNode, selected: MutableMap<PsiElement, Boolean>) {
        if (node.userObject is PsiElement && node.userObject !is PsiFile) {
            selected[node.userObject as PsiElement] = node.isChecked
        }
        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i)
            if (child is CheckedTreeNode) {
                collectSelectedElements(child, selected)
            }
        }
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return tree
    }

    private class MyTreeCellRenderer(private val existingKeys: Map<PsiElement, String>) : CheckboxTree.CheckboxTreeCellRenderer(true) {
        override fun customizeRenderer(
            tree: JTree,
            value: Any?,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ) {
            val node = value as? CheckedTreeNode ?: return
            val userObject = node.userObject
            val text = when (userObject) {
                is PsiFile -> userObject.name
                is PsiElement -> {
                    val key = existingKeys[userObject]
                    if (key != null) {
                        "'${userObject.text.take(20)}...' -> $key"
                    } else {
                        userObject.text
                    }
                }
                is String -> userObject
                else -> "Project"
            }
            this.textRenderer.append(text)
        }
    }
}