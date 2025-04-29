package com.chizh.questeditor

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import kotlinx.serialization.json.Json
import java.io.File

class RunFragment : Fragment() {

    private lateinit var titleText: TextView
    private lateinit var primaryMediaContainer: FrameLayout
    private lateinit var secondaryMediaContainer: FrameLayout
    private lateinit var editAnswer: EditText
    private lateinit var submitAnswer: Button

    private val model: ProjectViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_run, container, false)

        titleText = view.findViewById(R.id.title) as TextView
        primaryMediaContainer = view.findViewById(R.id.primary_media_container) as FrameLayout
        secondaryMediaContainer = view.findViewById(R.id.secondary_media_container) as FrameLayout
        editAnswer = view.findViewById(R.id.enter_answer) as EditText
        submitAnswer = view.findViewById(R.id.submit_button) as Button

        val projectFile = arguments?.getString("projectFile")
        if (projectFile != null) {
            val file = File(requireContext().filesDir, projectFile)
            if (file.exists()) {
                val json = file.readText()
                val screens = Json.decodeFromString<Project>(json).screens
                model.questScreens.clear()
                model.questScreens.addAll(screens)
            }
            else {
                titleText.setText("Не удалось загрузить квест :(")
            }
        }
        else {
            titleText.setText("Не удалось загрузить квест :(")
        }
        model.currentIndex = 0
        updateUI()

        submitAnswer.setOnClickListener { //при нажатии на кнопку "Вперёд" данные с текущего экрана сохраняются и интерфейс заполняется значениями из следующего элемента списка
            if (editAnswer.text.toString().trim().equals(model.questScreens[model.currentIndex].correctAnswer.trim(), ignoreCase = true)) {
                model.currentIndex++
                if (model.currentIndex >= model.questScreens.size) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Конец")
                        .setMessage("Поздравляем! Вы прошли квест!")
                        .setPositiveButton("Выйти") {_, _, ->
                            requireActivity().supportFragmentManager.popBackStack()
                        }.show()
                }
                else {
                    updateUI()
                }
            }
            else {
                Toast.makeText(context, "Ответ неверный!", Toast.LENGTH_SHORT).show()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showExitConfirmationDialog()
                }
            }
        )

        return view
    }

    private fun updateUI() {
        val screen = model.questScreens[model.currentIndex]
        titleText.setText(screen.screenText)
        editAnswer.setText("")

        primaryMediaContainer.removeAllViews()
        secondaryMediaContainer.removeAllViews()

        if (screen.primaryMediaType == MediaType.TEXT) {
            primaryMediaContainer.addView(createTextMediaSlot(screen.primaryMediaContent))
        }
        if (screen.secondaryMediaType == MediaType.TEXT) {
            secondaryMediaContainer.addView(createTextMediaSlot(screen.secondaryMediaContent))
        }

        if (screen.primaryMediaType == MediaType.IMAGE) {
            loadImage(Slot.PRIMARY, screen.primaryMediaContent)
        }
        if (screen.secondaryMediaType == MediaType.IMAGE) {
            loadImage(Slot.SECONDARY, screen.secondaryMediaContent)
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
                    loadImageIntoContainer(primaryMediaContainer, uri)
                }
                Slot.SECONDARY -> {
                    loadImageIntoContainer(secondaryMediaContainer, uri)
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
                    primaryMediaContainer.addView(placeholder)
                }
                Slot.SECONDARY -> {
                    secondaryMediaContainer.addView(placeholder)
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

                Glide.with(this@RunFragment)
                    .load(uri)
                    .override(targetWidth, Target.SIZE_ORIGINAL)
                    .into(imageView)

                container.addView(imageView)
            }
        })
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Выход из квеста")
            .setMessage("Вы точно хотите выйти из квеста? Текущий прогресс будет утерян")
            .setPositiveButton("Выйти") {_, _, ->
                requireActivity().supportFragmentManager.popBackStack()
            }.setNegativeButton("Отмена", null).show()
    }

}