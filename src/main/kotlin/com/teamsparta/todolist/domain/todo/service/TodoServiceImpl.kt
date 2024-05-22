package com.teamsparta.todolist.domain.todo.service

import com.teamsparta.todolist.domain.todo.dto.*
import com.teamsparta.todolist.domain.todo.exception.DisagreementException
import com.teamsparta.todolist.domain.todo.exception.ModelNotFoundException
import com.teamsparta.todolist.domain.todo.model.Comment
import com.teamsparta.todolist.domain.todo.model.Todo
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import com.teamsparta.todolist.domain.todo.model.toResponse
import com.teamsparta.todolist.domain.todo.repository.CommentRepository
import com.teamsparta.todolist.domain.todo.repository.TodoRepository

@Service
class TodoServiceImpl(
    private val todoRepository: TodoRepository,
    private val commentRepository: CommentRepository
) : TodoService {
    override fun getAllTodoList(): List<TodoResponse> {
        return todoRepository.findAllByOrderByDateDesc().map { it.toResponse() }
    }

    override fun getTodoById(todoId: Long): TodoResponse {
        val todo = todoRepository.findByIdOrNull(todoId) ?: throw ModelNotFoundException("Todo", todoId)
        return todo.toResponse()
    }

    override fun createTodo(request: CreateTodoRequest): TodoResponse {

        val todo = Todo(
            title = request.title,
            description = request.description,
            name = request.name,
        )
        return todoRepository.save(todo).toResponse()
    }

    @Transactional
    override fun updateTodo(todoId: Long, request: UpdateTodoRequest): TodoResponse {

        val todo = todoRepository.findByIdOrNull(todoId) ?: throw ModelNotFoundException("Todo", todoId)
        todo.title = request.title
        todo.description = request.description
        todo.name = request.name

        return todoRepository.save(todo).toResponse()
    }

    override fun changeTodoStatus(todoId: Long): TodoResponse {
        val todo = todoRepository.findByIdOrNull(todoId) ?: throw ModelNotFoundException("Todo", todoId)
        todo.status = !todo.status
        return todoRepository.save(todo).toResponse()
    }

    @Transactional
    override fun deleteTodo(todoId: Long) {
        val todo = todoRepository.findByIdOrNull(todoId) ?: throw ModelNotFoundException("Todo", todoId)
        todoRepository.delete(todo)
    }

    @Transactional
    override fun createComment(todoId: Long, request: CreateCommentRequest): CommentResponse {
        val todo = todoRepository.findByIdOrNull(todoId) ?: throw ModelNotFoundException("Todo", todoId)

        val comment = Comment(
            commentWriter = request.commentWriter,
            password = request.password,
            comment = request.comment,
            todo = todo
        ).let { commentRepository.save(it) }
        todo.addComment(comment)
        todoRepository.save(todo)
        return comment.toResponse()
    }

    @Transactional
    override fun updateComment(todoId: Long, commentId: Long, request: UpdateCommentRequest): CommentResponse {
        val comment =
            commentRepository.findByTodoIdAndId(todoId, commentId) ?: throw ModelNotFoundException("Comment", commentId)
        comment.comment = request.comment
        if (comment.commentWriter != request.commentWriter) throw DisagreementException("commentWriter")
        if (comment.password != request.password) throw DisagreementException("password")

        return commentRepository.save(comment).toResponse()
    }

    @Transactional
    override fun deleteComment(todoId: Long, commentId: Long, request: DeleteCommentRequest) {
        if (!todoRepository.existsById(todoId)) throw ModelNotFoundException("Todo", todoId)
        val comment =
            commentRepository.findByIdOrNull(commentId) ?: throw ModelNotFoundException("Comment", commentId)
        if (comment.commentWriter != request.commentWriter) throw DisagreementException("commentWriter")
        if (comment.password != request.password) throw DisagreementException("password")

        commentRepository.delete(comment)
    }
}