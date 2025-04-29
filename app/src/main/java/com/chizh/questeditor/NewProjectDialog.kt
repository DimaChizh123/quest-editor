package com.chizh.questeditor

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.DialogFragment

class NewProjectDialog : DialogFragment() {
    //создание класса диалога (onCreateProject - функция, которая вызовется при нажатии кнопки "Создать")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog { //функция вызывается когда надо показать диалог
        val view = layoutInflater.inflate(R.layout.dialog_new_project, null) //создаём view и ни к чему его не привязываем
        val input = view.findViewById<EditText>(R.id.enter_project_title) //создаём экземпляр EditText

        return AlertDialog.Builder(context) //строитель диалога
            .setTitle("Введите название проекта") //заголовок диалога
            .setView(view) //содержимое диалога
            .setPositiveButton("Создать") {_, _ -> //вообще эти аргументы нужны для управления поведением диалога и определением, какая кнопка нажата, но они нам сейчас не нужны
                val result = Bundle().apply { //создаём Bundle и пихаем туда то, что мы вводили в input для дальнейшей передачи
                    putString("project_title", input.text.toString().ifBlank { "Без названия" })
                }
                setFragmentResult("new_project_result", result) //передача нашего Bundle
            }.setNegativeButton("Отмена", null).create()
    }
}