package com.chizh.questeditor

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File


class MenuFragment : Fragment() {

    private lateinit var createButton: Button
    private lateinit var projectList: RecyclerView
    private var adapter: ProjectListAdapter? = null
    private lateinit var projects: MutableList<ProjectInfo>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_menu, container, false)

        createButton = view.findViewById(R.id.create_project_button) as Button
        projectList = view.findViewById(R.id.project_list) as RecyclerView
        projectList.layoutManager = LinearLayoutManager(context)

        createButton.setOnClickListener {
            val dialog = NewProjectDialog()
            dialog.show(parentFragmentManager, "NewProjectDialog")
            //parentFragmentManager - менеджер фрагментов, к которому привязан этот фрагмент
            //то есть здесь говорится о том, чтобы диалог вызывался из менеджера фрагментов MenuFragment, а не Activity

        }

        parentFragmentManager.setFragmentResultListener("new_project_result", viewLifecycleOwner) {_, bundle -> //слушатель на результат из диалога
            //viewLifecycleOwner - сколько должен жить слушатель (конкретно здесь - столько, сколько живёт фрагмент)
            val title = bundle.getString("project_title") ?: "Без названия" //получение от Bundle того, что мы вводили в EditText будучи в диалоге
            val projectFile = createProject()
            val newProject = ProjectInfo(title, projectFile, System.currentTimeMillis())
            projects.add(0, newProject)
            saveProjectsToFile()
            val fragment = ProjectMenuFragment.newInstance(newProject.projectFile, newProject.title)
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
            updateUI()
        }

        projects = loadProjectsFromFile()
        updateUI()

        return view
    }

    private fun updateUI() {
        adapter = ProjectListAdapter(projects)
        projectList.adapter = adapter
    }

    private fun saveProjectsToFile() { //сохранение проекта в json
        val jsonProjects = Json.encodeToString(projects)
        File(requireContext().filesDir, "projects_list.json").writeText(jsonProjects)
    }

    private fun loadProjectsFromFile(): MutableList<ProjectInfo> { //чтение проекта из файла (при офибке или несуществовании файла возвращаем просто пустой список)
        try {
            val file = File(requireContext().filesDir, "projects_list.json")
            if (file.exists()) {
                val jsonProjects = file.readText()
                return Json.decodeFromString<List<ProjectInfo>>(jsonProjects).toMutableList()
            }
            else {
                return mutableListOf()
            }
        }
        catch (e: Exception) {
            e.printStackTrace()
            return mutableListOf()
        }
    }

    private fun createProject(): String { //создание абсолютно пустого проекта
        val project = Project(listOf(QuestScreen("Новый проект")))
        val projectFile = "project_${System.currentTimeMillis()}.json"
        val json = Json.encodeToString(project)
        File(requireContext().filesDir, projectFile).writeText(json)
        return projectFile
    }

    private inner class ProjectViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private lateinit var project: ProjectInfo

        private val titleTextView: TextView = itemView.findViewById(R.id.item_project_title)
        private val dateTextView: TextView = itemView.findViewById(R.id.item_project_date)

        fun bind(project: ProjectInfo) {
            this.project = project
            titleTextView.text = this.project.title
            dateTextView.text = DateFormat.format("dd.MM.yyyy HH:mm", this.project.date)

            itemView.setOnClickListener {
                val fragment = ProjectMenuFragment.newInstance(project.projectFile, project.title)
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
                //замена текущего фрагмента на новый и сохранение текущего в стек
            }
        }
    }

    private inner class ProjectListAdapter(val projects: List<ProjectInfo>) : RecyclerView.Adapter<ProjectViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
            val view = layoutInflater.inflate(R.layout.item_project, parent, false)
            return ProjectViewHolder(view)
        }

        override fun getItemCount() = projects.size

        override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
            val project = projects[position]
            holder.bind(project)
        }

    }

}