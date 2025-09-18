package de.keeyzar.gpthelper.gpthelper.features.setup.presentation.widgets

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiElement
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.tree.TreeUtil
import com.jetbrains.lang.dart.psi.DartCallExpression
import de.keeyzar.gpthelper.gpthelper.features.setup.domain.service.ProjectFileChange
import java.awt.BorderLayout
import java.awt.Dimension
import java.nio.file.Paths
import javax.swing.*
import javax.swing.tree.DefaultTreeModel

/**
 * Dialog that shows multiple proposed file changes (original vs modified) and allows the user
 * to select which changes to apply. Uses IntelliJ's DiffManager to render diffs in the center pane.
 */
class ProjectFileChangesDialog(
    private val project: Project,
    private val changes: List<ProjectFileChange>
) : DialogWrapper(project) {

    private val rootNode = CheckedTreeNode(project.name)
    private lateinit var tree: CheckboxTree
    private lateinit var diffViewerComponent: JComponent

    init {
        title = "Modify Application Files"
        setOKButtonText("Modify Selected")
        buildTree()
        super.init()
    }

    override fun createCenterPanel(): JComponent {
        val leftPanel = createTreePanel()
        val centerPanel = createDiffPanel()

        tree.addTreeSelectionListener {
            val selectedNode = tree.lastSelectedPathComponent as? CheckedTreeNode
            val userObject = selectedNode?.userObject
            if (userObject is ProjectFileChange) {
                updateDiffView(userObject)
            }
        }

        TreeUtil.expandAll(tree)

        val firstCallNode = findFirstChangeNode(rootNode)
        if (firstCallNode != null) {
            TreeUtil.selectNode(tree, firstCallNode)
        }

        return JBSplitter(false, 0.3f).apply {
            firstComponent = leftPanel
            secondComponent = centerPanel
            preferredSize = Dimension(1000, 600)
        }
    }

    private fun createTreePanel(): JComponent {
        tree = CheckboxTree(MyTreeCellRenderer(), rootNode)
        tree.model = DefaultTreeModel(rootNode)

        val selectAllButton = JButton("Select All")
        selectAllButton.addActionListener { setAllChecked(true) }

        val unselectAllButton = JButton("Unselect All")
        unselectAllButton.addActionListener { setAllChecked(false) }

        val expandAllButton = JButton("Expand All")
        expandAllButton.addActionListener { TreeUtil.expandAll(tree) }

        val collapseAllButton = JButton("Collapse All")
        collapseAllButton.addActionListener { TreeUtil.collapseAll(tree, 1) }

        val buttonPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(selectAllButton)
            add(unselectAllButton)
            add(expandAllButton)
            add(collapseAllButton)
            add(Box.createHorizontalGlue())
        }

        return JPanel(BorderLayout()).apply {
            add(buttonPanel, BorderLayout.NORTH)
            add(JBScrollPane(tree), BorderLayout.CENTER)
        }
    }

    private fun createDiffPanel(): JComponent {
        val diffViewer = DiffManager.getInstance().createRequestPanel(project, {}, null)
        diffViewerComponent = diffViewer.component
        return JPanel(BorderLayout()).apply {
            add(diffViewerComponent, BorderLayout.CENTER)
        }
    }

    private fun setAllChecked(value: Boolean) {
        setNodeCheckedRecursive(rootNode, value)
        tree.repaint()
    }

    private fun setNodeCheckedRecursive(node: CheckedTreeNode, value: Boolean) {
        node.isChecked = value
        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i)
            if (child is CheckedTreeNode) setNodeCheckedRecursive(child, value)
        }
    }

    /**
     * Extracts a project-relative file path from a PSI element reference.
     * This is the single source of truth for the file path within this dialog.
     */
    private fun getPathFromReference(reference: Any): String? {
        val psiElement = reference as? PsiElement ?: return null
        val virtualFile = psiElement.containingFile?.virtualFile ?: return null
        val projectBasePath = project.basePath ?: return virtualFile.path
        val projectPath = Paths.get(projectBasePath)
        val filePath = Paths.get(virtualFile.path)
        return try {
            projectPath.relativize(filePath).toString().replace("\\", "/") // Normalize to forward slashes
        } catch (e: IllegalArgumentException) {
            virtualFile.path
        }
    }

    private fun buildTree() {
        val nodeMap = mutableMapOf<String, CheckedTreeNode>()

        for (change in changes) {
            val relativePath = getPathFromReference(change.reference) ?: continue
            val pathComponents = relativePath.split('/')
            val dirComponents = pathComponents.dropLast(1)
            val fileName = pathComponents.lastOrNull() ?: continue

            var currentParentNode = rootNode
            var currentPath = ""

            for (dirPart in dirComponents) {
                currentPath = if (currentPath.isEmpty()) dirPart else "$currentPath/$dirPart"
                currentParentNode = nodeMap.getOrPut(currentPath) {
                    val newNode = CheckedTreeNode(dirPart)
                    currentParentNode.add(newNode)
                    newNode
                }
            }

            val filePath = if (currentPath.isEmpty()) fileName else "$currentPath/$fileName"
            val fileNode = nodeMap.getOrPut(filePath) {
                val newNode = CheckedTreeNode(fileName)
                currentParentNode.add(newNode)
                newNode
            }

            val appNode = CheckedTreeNode(change)
            appNode.isChecked = true
            fileNode.add(appNode)
        }
    }

    private fun findFirstChangeNode(node: CheckedTreeNode): CheckedTreeNode? {
        if (node.userObject is ProjectFileChange) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i) as? CheckedTreeNode ?: continue
            val found = findFirstChangeNode(child)
            if (found != null) return found
        }
        return null
    }

    private fun updateDiffView(change: ProjectFileChange) {
        try {
            val diffContentFactory = DiffContentFactory.getInstance()
            val content1 = diffContentFactory.create(change.original)
            val content2 = diffContentFactory.create(change.modified, PlainTextFileType.INSTANCE)

            // CORRECTED: Derive the path from the reference for the diff title
            val pathForTitle = getPathFromReference(change.reference) ?: "Modified File"
            val request = SimpleDiffRequest(pathForTitle, content1, content2, "Original", "Modified")

            val diffViewer = DiffManager.getInstance().createRequestPanel(project, {}, null)
            diffViewer.setRequest(request)

            val parent = diffViewerComponent.parent
            parent.remove(diffViewerComponent)
            diffViewerComponent = diffViewer.component
            parent.add(diffViewerComponent, BorderLayout.CENTER)
            parent.revalidate()
            parent.repaint()
        } catch (_: Exception) {
            // Log the error or handle it appropriately
        }
    }

    fun getSelectedChanges(): List<ProjectFileChange> {
        val selected = mutableListOf<ProjectFileChange>()
        collectSelectedChanges(rootNode, selected)
        return selected
    }

    private fun collectSelectedChanges(node: CheckedTreeNode, out: MutableList<ProjectFileChange>) {
        if (node.isChecked && node.userObject is ProjectFileChange) {
            out.add(node.userObject as ProjectFileChange)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i)
            if (child is CheckedTreeNode) collectSelectedChanges(child, out)
        }
    }

    override fun doOKAction() {
        if (getSelectedChanges().isNotEmpty()) super.doOKAction() else {
            JOptionPane.showMessageDialog(null, "Please select at least one file to modify.", "No Selection", JOptionPane.ERROR_MESSAGE)
        }
    }

    companion object {
        private fun inferCallName(change: ProjectFileChange): String {
            val repr = try { (change.reference as DartCallExpression).firstChild.text } catch (_: Exception) { "" }
            if (repr.contains("MaterialApp")) return "MaterialApp"
            if (repr.contains("CupertinoApp")) return "CupertinoApp"
            // CORRECTED: Removed the fallback that used the non-existent 'displayPath'
            return "CallExpression"
        }
    }

    private class MyTreeCellRenderer : CheckboxTree.CheckboxTreeCellRenderer(true) {
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
                is ProjectFileChange -> inferCallName(userObject)
                is String -> userObject
                else -> userObject?.toString() ?: "Project"
            }
            this.textRenderer.append(text)
        }
    }
}