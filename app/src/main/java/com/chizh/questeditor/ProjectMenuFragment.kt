package com.chizh.questeditor

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class ProjectMenuFragment : Fragment() {

    private lateinit var titleText: TextView
    private lateinit var projectPreview: ScrollView
    private lateinit var previewText: TextView
    private lateinit var previewPrimaryContainer: FrameLayout
    private lateinit var previewSecondaryContainer: FrameLayout
    private lateinit var runButton: Button
    private lateinit var editButton: Button
    private lateinit var deleteButton: Button

    private lateinit var firstScreen: QuestScreen

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_project_menu, container, false)

        titleText = view.findViewById(R.id.project_title) as TextView
        projectPreview = view.findViewById(R.id.project_preview_container) as ScrollView
        previewText = view.findViewById(R.id.title) as TextView
        previewPrimaryContainer = view.findViewById(R.id.primary_media_container) as FrameLayout
        previewSecondaryContainer = view.findViewById(R.id.secondary_media_container) as FrameLayout
        runButton = view.findViewById(R.id.run_project_button) as Button
        editButton = view.findViewById(R.id.edit_project_button) as Button
        deleteButton = view.findViewById(R.id.delete_project_button) as Button

        val title = arguments?.getString("title") ?: "Не найдено"
        titleText.text = title
        val projectFile = arguments?.getString("projectFile") ?: return view //получение данных из arguments и возврат почти пустого экрана при отсутствии arguments
        //projectFile - путь к файлу нашего проекта

        val file = File(requireContext().filesDir, projectFile)
        if (file.exists()) {
            val json = file.readText()
            val screens = Json.decodeFromString<Project>(json).screens
            firstScreen = screens[0]
        }
        else {
            titleText.setText("Не удалось загрузить квест :(")
        }

        setPreview()

        runButton.setOnClickListener {
            val fragment = RunFragment()
            fragment.arguments = Bundle().apply {
                putString("projectFile", projectFile)
            }
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }

        editButton.setOnClickListener {
            val fragment = EditorFragment()
            fragment.arguments = Bundle().apply {
                putString("projectFile", projectFile)
            }
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }

        deleteButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Удалить проект?")
                .setMessage("Проект будет удалён без возможности восстановления")
                .setPositiveButton("Удалить") {_, _, -> deleteProject(projectFile)}.setNegativeButton("Отмена", null).show()
        }

        return view
    }

    private fun setPreview() {
        previewText.setText(firstScreen.screenText)

        if (firstScreen.primaryMediaType == MediaType.TEXT) {
            previewPrimaryContainer.addView(createTextMediaSlot(firstScreen.primaryMediaContent))
        }
        if (firstScreen.secondaryMediaType == MediaType.TEXT) {
            previewSecondaryContainer.addView(createTextMediaSlot(firstScreen.secondaryMediaContent))
        }

        if (firstScreen.primaryMediaType == MediaType.IMAGE) {
            loadImage(Slot.PRIMARY, firstScreen.primaryMediaContent)
        }
        if (firstScreen.secondaryMediaType == MediaType.IMAGE) {
            loadImage(Slot.SECONDARY, firstScreen.secondaryMediaContent)
        }
    }

    private fun createTextMediaSlot(content: String?): TextView {
        val textView = TextView(requireContext()).apply {
            setText(content ?: "")
            layoutParams = FrameLayout.LayoutParams (
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }
        return textView
    }

    private fun loadImage(slot: Slot, content: String?) {
        if (!content.isNullOrEmpty()) {
            val uri = content.toUri()
            when (slot) {
                Slot.PRIMARY -> {
                    loadImageIntoContainer(previewPrimaryContainer, uri)
                }
                Slot.SECONDARY -> {
                    loadImageIntoContainer(previewSecondaryContainer, uri)
                }
            }
        }
        else {
            val placeholder = TextView(requireContext()).apply {
                text = "Нет изображения"
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            }
            when (slot) {
                Slot.PRIMARY -> {
                    previewPrimaryContainer.addView(placeholder)
                }
                Slot.SECONDARY -> {
                    previewSecondaryContainer.addView(placeholder)
                }
            }
        }
    }

    private fun loadImageIntoContainer(container: FrameLayout, uri: Uri) {
        container.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                container.viewTreeObserver.removeOnGlobalLayoutListener(this)

                val targetWidth = container.width.takeIf { it > 0 } ?: 500

                val imageView = ImageView(requireContext()).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, // ширина контейнера
                        FrameLayout.LayoutParams.WRAP_CONTENT  // высота автоматическая
                    )
                    adjustViewBounds = true
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    tag = uri.toString()
                }

                Glide.with(this@ProjectMenuFragment)
                    .load(uri)
                    .override(targetWidth, Target.SIZE_ORIGINAL)
                    .into(imageView)

                container.addView(imageView)
            }
        })
    }

    private fun deleteProject(projectFile: String) {
        val file = File(requireContext().filesDir, projectFile)
        file.delete() //удаление файла проекта

        val projectsFile = File(requireContext().filesDir, "projects_list.json") //ссылка на файл со списком проектов
        val updatedProjects = if (projectsFile.exists()) { //проверка файла на существование
            val json = projectsFile.readText() //считывание содержимого файла
            Json.decodeFromString<List<ProjectInfo>>(json).toMutableList().filterNot {it.projectFile == projectFile} //расшифровка json файла в список проектов и удаление из этого списка того проекта, чей путь к файлу равен нашему путю к файлу
        }
        else {
            emptyList() //если файла со списком проектов не существует - берём пустой список
        }
        val updatedJson = Json.encodeToString(updatedProjects) //превращаем список проектов в json
        projectsFile.writeText(updatedJson) //и сохраняем

        parentFragmentManager.popBackStack() //убираем текущий фрагмент из стека и возвращаемся к предыдущему
    }

    companion object {
        fun newInstance(projectFile: String, title: String): ProjectMenuFragment { //создание фрагмента и передача ему данных
            val fragment = ProjectMenuFragment()
            val args = Bundle().apply { //создание Bundle и передача в него данных
                putString("projectFile", projectFile)
                putString("title", title)
            }
            fragment.arguments = args //свойство arguments хранит Bundle, создаваемый при создании Fragment (в отличие от savedInstanceState, который пересоздаётся каждый раз)
            return fragment //возвращение настроенного фрагмента
        }
    }
}