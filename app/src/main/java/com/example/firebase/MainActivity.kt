package com.example.firebase // <--- SUBSTITUA PELO SEU PACOTE (OLHE A 1ª LINHA DO SEU ARQUIVO ORIGINAL)

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

// Modelo de Dados
data class Task(
    val id: String = "",
    val title: String = "",
    val isCompleted: Boolean = false
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                FireTasksApp()
            }
        }
    }
}

@Composable
fun FireTasksApp() {
    // Estado para o texto da nova tarefa
    var newTaskText by remember { mutableStateOf("") }

    // Estado para a lista de tarefas
    val taskList = remember { mutableStateListOf<Task>() }

    // Conexão com o Firestore
    val db = Firebase.firestore

    // OUVINTE EM TEMPO REAL (Versão Manual/Corrigida)
    LaunchedEffect(Unit) {
        db.collection("tasks").addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.w("FireTasks", "Erro de conexão", e)
                return@addSnapshotListener
            }

            if (snapshots != null) {
                taskList.clear() // Limpa a lista visual para recriar com dados novos

                for (document in snapshots) {
                    // AQUI ESTÁ A CORREÇÃO:
                    // Lemos cada campo manualmente pelo nome exato que está no site
                    val id = document.id
                    val title = document.getString("title") ?: "Sem título"

                    // Forçamos a leitura do booleano, se for nulo, assume falso
                    val isCompleted = document.getBoolean("isCompleted") ?: false

                    taskList.add(Task(id, title, isCompleted))
                }
            }
        }
    }

    // Layout da Tela
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Fire Tasks",
            style = MaterialTheme.typography.h4,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 1. CRIAR (Create)
        Row(modifier = Modifier.fillMaxWidth()) {
            TextField(
                value = newTaskText,
                onValueChange = { newTaskText = it },
                label = { Text("Nova tarefa") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (newTaskText.isNotBlank()) {
                        // Cria o mapa de dados para enviar
                        val taskMap = hashMapOf(
                            "title" to newTaskText,
                            "isCompleted" to false
                        )
                        db.collection("tasks").add(taskMap)
                        newTaskText = "" // Limpa o campo
                    }
                },
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Text("Add")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. LER (Read) - Lista
        LazyColumn {
            items(taskList) { task ->
                TaskItem(
                    task = task,
                    onToggle = { isChecked ->
                        // 3. ATUALIZAR (Update)
                        Log.d("FireTasks", "Atualizando tarefa ${task.title} para $isChecked")
                        db.collection("tasks").document(task.id)
                            .update("isCompleted", isChecked)
                    },
                    onDelete = {
                        // 4. EXCLUIR (Delete)
                        db.collection("tasks").document(task.id).delete()
                    }
                )
            }
        }
    }
}

@Composable
fun TaskItem(task: Task, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    Card(
        elevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            // Checkbox
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { isChecked -> onToggle(isChecked) }
            )

            // Texto da Tarefa
            Text(
                text = task.title,
                modifier = Modifier.weight(1f),
                // Risca o texto se estiver concluído (opcional visual)
                style = if (task.isCompleted)
                    LocalTextStyle.current.copy(color = Color.Gray)
                else
                    LocalTextStyle.current
            )

            // Botão Excluir
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Excluir")
            }
        }
    }
}