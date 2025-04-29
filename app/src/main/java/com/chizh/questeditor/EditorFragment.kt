package com.chizh.questeditor

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class EditorFragment : Fragment() {

    private lateinit var screenNumber : TextView
    private lateinit var editTitle: EditText
    private lateinit var editAnswer: EditText
    private lateinit var previousButton: Button
    private lateinit var deleteButton: Button
    private lateinit var nextButton: Button
    private lateinit var addButton: Button
    private lateinit var primaryMediaContainer: FrameLayout
    private lateinit var secondaryMediaContainer: FrameLayout

    private val model: ProjectViewModel by viewModels()

    private var currentPrimaryEditText: EditText? = null
    private var currentSecondaryEditText: EditText? = null
    private var currentPrimaryImageView: ImageView? = null
    private var currentSecondaryImageView: ImageView? = null

    private var imageSlot: Slot? = null
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val imageUri: Uri? = data?.data
            setImageToSlot(imageSlot, imageUri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_editor, container, false)

        screenNumber = view.findViewById(R.id.screen_number) as TextView
        editTitle = view.findViewById(R.id.enter_title) as EditText
        editAnswer = view.findViewById(R.id.enter_answer) as EditText
        previousButton = view.findViewById(R.id.button_previous) as Button
        deleteButton = view.findViewById(R.id.button_delete) as Button
        nextButton = view.findViewById(R.id.button_next) as Button
        addButton = view.findViewById(R.id.button_add) as Button
        primaryMediaContainer = view.findViewById(R.id.primary_media_container) as FrameLayout
        secondaryMediaContainer = view.findViewById(R.id.secondary_media_container) as FrameLayout

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
                model.questScreens.add(QuestScreen())
            }
        }
        else {
            model.questScreens.add(QuestScreen())
        }

        if (model.questScreens.isEmpty()) {
            model.questScreens.add(QuestScreen())
        }

        updateUI()

        primaryMediaContainer.setOnClickListener { //при нажатии на FrameLayout выскакивает диалог с выбором действий
            saveCurrentScreen()
            showEditDialog(Slot.PRIMARY)
        }

        secondaryMediaContainer.setOnClickListener { //при нажатии на FrameLayout выскакивает диалог с выбором действий
            saveCurrentScreen()
            showEditDialog(Slot.SECONDARY)
        }

        previousButton.setOnClickListener { //при нажатии на кнопку "Назад" данные с текущего экрана сохраняются и интерфейс заполняется значениями из прошлого элемента списка
            saveCurrentScreen()
            if (model.currentIndex > 0) {
                model.currentIndex--
                updateUI()
            }
            else {
                Toast.makeText(context, "Вы находитесь на первом экране", Toast.LENGTH_SHORT).show()
            }
        }

        nextButton.setOnClickListener { //при нажатии на кнопку "Вперёд" данные с текущего экрана сохраняются и интерфейс заполняется значениями из следующего элемента списка
            saveCurrentScreen()
            if (model.currentIndex >= model.questScreens.size - 1) {
                model.questScreens.add(QuestScreen())
            }
            model.currentIndex++
            updateUI()
        }

        addButton.setOnClickListener {
            saveCurrentScreen()
            AlertDialog.Builder(requireContext())
                .setTitle("Где вставить новый экран?")
                .setPositiveButton("Справа") { _, _, ->
                    model.questScreens.add(model.currentIndex + 1, QuestScreen())
                    model.currentIndex++
                    updateUI()
                }
                .setNegativeButton("Слева") {_, _, ->
                    model.questScreens.add(model.currentIndex, QuestScreen())
                    updateUI()
                }
                .setNeutralButton("Отмена", null).show()
        }

        deleteButton.setOnClickListener { //при нажатии на кнопку "Удалить" текущий элемент списка экранов удаляется и интерфейс заполняется значениями из прошлого элемента списка
            AlertDialog.Builder(requireContext())
                .setTitle("Удалить экран")
                .setMessage("Вы точно хотите удалить текущий экран?")
                .setPositiveButton("Удалить") {_, _, ->
                    if (model.questScreens.size > 1) {
                        model.questScreens.removeAt(model.currentIndex)
                        if (model.currentIndex > 0) model.currentIndex--
                    }
                    else {
                        model.questScreens.clear()
                        model.questScreens.add(QuestScreen())
                        primaryMediaContainer.removeAllViews()
                        secondaryMediaContainer.removeAllViews()
                        currentPrimaryEditText = null
                        currentSecondaryEditText = null
                        currentPrimaryImageView = null
                        currentSecondaryImageView = null
                    }
                    updateUI()
                }.setNegativeButton("Отмена", null).show()

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

    private fun saveCurrentScreen() { //сохранение введённых данных в список экранов
        val current = model.questScreens[model.currentIndex]
        current.screenText = editTitle.text.toString()
        current.correctAnswer = editAnswer.text.toString()

        //Если EditText существует - сохраняем его текст и тип
        if (current.primaryMediaType == MediaType.TEXT) {
            currentPrimaryEditText?.let {
                current.primaryMediaContent = it.text.toString()
            }
        }
        if (current.secondaryMediaType == MediaType.TEXT) {
            currentSecondaryEditText?.let {
                current.secondaryMediaContent = it.text.toString()
            }
        }

        if (current.primaryMediaType == MediaType.IMAGE) {
            currentPrimaryImageView?.let {
                current.primaryMediaContent = it.tag?.toString()
            }
        }
        if (current.secondaryMediaType == MediaType.IMAGE) {
            currentSecondaryImageView?.let {
                current.secondaryMediaContent = it.tag?.toString()
            }
        }
    }

    private fun updateUI() { //заполнение элементов интерфейса данными из списка экранов
        val screen = model.questScreens[model.currentIndex]
        screenNumber.setText("Экран ${model.currentIndex + 1} / ${model.questScreens.size}")
        editTitle.setText(screen.screenText)
        editAnswer.setText(screen.correctAnswer)

        //изначально все медиаслоты пустые
        primaryMediaContainer.removeAllViews()
        secondaryMediaContainer.removeAllViews()

        //Если в слоте должен храниться текст, то его наполняют текстом
        if (screen.primaryMediaType == MediaType.TEXT) {
            currentPrimaryEditText = createTextMediaSlot(screen.primaryMediaContent)
            primaryMediaContainer.addView(currentPrimaryEditText)
        } else {
            currentPrimaryEditText = null
        }
        if (screen.secondaryMediaType == MediaType.TEXT) {
            currentSecondaryEditText = createTextMediaSlot(screen.secondaryMediaContent)
            secondaryMediaContainer.addView(currentSecondaryEditText)
        } else {
            currentSecondaryEditText = null
        }

        if (screen.primaryMediaType == MediaType.IMAGE) {
            loadImage(Slot.PRIMARY, screen.primaryMediaContent)
        } else {
            currentPrimaryImageView = null
        }

        if (screen.secondaryMediaType == MediaType.IMAGE) {
            loadImage(Slot.SECONDARY, screen.secondaryMediaContent)
        } else {
            currentSecondaryImageView = null
        }
    }

    private fun loadImage(slot: Slot, content: String?) {
        if (!content.isNullOrEmpty()) {
            val uri = content.toUri()
            when (slot) {
                Slot.PRIMARY -> {
                    loadImageIntoContainer(primaryMediaContainer, uri, slot)
                }
                Slot.SECONDARY -> {
                    loadImageIntoContainer(secondaryMediaContainer, uri, slot)
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

    private fun loadImageIntoContainer(container: FrameLayout, uri: Uri, slot: Slot) {
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

                Glide.with(this@EditorFragment)
                    .load(uri)
                    .override(targetWidth, Target.SIZE_ORIGINAL)
                    .into(imageView)

                container.addView(imageView)

                when (slot) {
                    Slot.PRIMARY -> currentPrimaryImageView = imageView
                    Slot.SECONDARY -> currentSecondaryImageView = imageView
                }
            }
        })
    }

    private fun createTextMediaSlot(content: String?): EditText {
        //создание, наполнение текстом и оформление EditText
        val editText = EditText(requireContext()).apply {
            setText(content ?: "")
            hint = "Введите текст"
            layoutParams = FrameLayout.LayoutParams (
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }
        return editText
    }

    private fun showEditDialog(slot: Slot) { //диалог с выбором действия для конкретного слота
        AlertDialog.Builder(requireContext())
            .setTitle("Выберите действие")
            .setItems(arrayOf("Добавить текст", "Добавить изображение", "Удалить")) {_, which ->
                when (which) {
                    0 -> insertTextToSlot(slot)
                    1 -> insertImageToSlot(slot)
                    2 -> clearSlot(slot)
                }
            }.show()
    }

    private fun insertTextToSlot(slot: Slot) {
        val screen = model.questScreens[model.currentIndex]
        screen.apply {
            if (slot == Slot.PRIMARY) {
                primaryMediaType = MediaType.TEXT
                primaryMediaContent = null
            }
            else {
                secondaryMediaType = MediaType.TEXT
                secondaryMediaContent = null
            }
        }
        updateUI()
    }

    private fun insertImageToSlot(slot: Slot) {
        imageSlot = slot
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun setImageToSlot(slot: Slot?, uri: Uri?) {
        slot ?: return
        val screen = model.questScreens[model.currentIndex]
        screen.apply {
            if (slot == Slot.PRIMARY) {
                primaryMediaType = MediaType.IMAGE
                primaryMediaContent = uri.toString()
            }
            else {
                secondaryMediaType = MediaType.IMAGE
                secondaryMediaContent = uri.toString()
            }
        }
        updateUI()
    }

    private fun clearSlot(slot: Slot) {
        val screen = model.questScreens[model.currentIndex]
        when (slot) {
            Slot.PRIMARY -> {
                screen.primaryMediaType = null
                screen.primaryMediaContent = null
                primaryMediaContainer.removeAllViews()
                currentPrimaryEditText = null
                currentPrimaryImageView = null
            }
            Slot.SECONDARY -> {
                screen.secondaryMediaType = null
                screen.secondaryMediaContent = null
                secondaryMediaContainer.removeAllViews()
                currentSecondaryEditText = null
                currentSecondaryImageView = null
            }
        }
        updateUI()
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Выход из редактора")
            .setMessage("Хотите сохранить изменения перед выходом?")
            .setPositiveButton("Сохранить и выйти") {_, _, ->
                saveCurrentScreen()
                val projectFile = arguments?.getString("projectFile")
                if (projectFile != null) {
                    val project = Project(model.questScreens)
                    val json = Json.encodeToString(project)
                    val file = File(requireContext().filesDir, projectFile)
                    file.writeText(json)
                }
                requireActivity().supportFragmentManager.popBackStack()
            }.setNegativeButton("Выйти без сохранения") {_, _, ->
                requireActivity().supportFragmentManager.popBackStack()
            }.setNeutralButton("Отмена", null).show()
    }
}