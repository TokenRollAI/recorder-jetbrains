package com.github.disdjj.recorderjetbrains.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory

class RecorderStatusBarWidgetFactory : StatusBarWidgetFactory {
    
    override fun getId(): String = RecorderStatusBarWidget.WIDGET_ID
    
    override fun getDisplayName(): String = "Recorder"
    
    override fun isAvailable(project: Project): Boolean = true
    
    override fun createWidget(project: Project): StatusBarWidget {
        return RecorderStatusBarWidget(project)
    }
    
    override fun disposeWidget(widget: StatusBarWidget) {
        widget.dispose()
    }
    
    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true
}
