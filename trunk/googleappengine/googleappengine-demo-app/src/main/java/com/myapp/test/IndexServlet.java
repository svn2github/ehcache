package com.myapp.test;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.myapp.test.model.Message;
import com.myapp.test.MessageRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexServlet extends HttpServlet {

	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory
			.getLogger(IndexServlet.class);

	private MessageRepository messageRepository = CachingMessageRepository.get();
	
	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		if (log.isDebugEnabled()) {
			log.debug("doGet");
		}
		
		// delete
		if (request.getParameter("id") != null) {
			deleteMessage(request);
			
			response.sendRedirect("index");
			return;
		}

		// get
		Collection<Message> messages = messageRepository.getAll();
		request.setAttribute("messages", messages);

		if (log.isDebugEnabled()) {
			log.debug("messages: " + messages);
		}

		forward(request, response, "index.jsp");
	}

	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		
		if (log.isDebugEnabled()) {
			log.debug("doPost");
		}
		
		// create
		createMessage(request);
		response.sendRedirect("index");
	}

	protected void createMessage(HttpServletRequest request) {
		String text = request.getParameter("text");
		if (log.isDebugEnabled()) {
			log.debug("creating message with text: " + text);
		}

		Message message = new Message();
		message.setText(text);
		messageRepository.create(message);
	}

	protected void deleteMessage(HttpServletRequest request) throws IOException {
		Long id = Long.valueOf(request.getParameter("id"));
		if (log.isDebugEnabled()) {
			log.debug("deleting message with id: " + id);
		}
		messageRepository.deleteById(id);
	}

	/**
	 * Forwards request and response to given path. Handles any exceptions
	 * caused by forward target by printing them to logger.
	 * 
	 * @param request 
	 * @param response
	 * @param path 
	 */
	protected void forward(HttpServletRequest request,
			HttpServletResponse response, String path) {
		try {
			RequestDispatcher rd = request.getRequestDispatcher(path);
			rd.forward(request, response);
		} catch (Throwable tr) {
			if (log.isErrorEnabled()) {
				log.error("Cought Exception: " + tr.getMessage());
				log.debug("StackTrace:", tr);
			}
		}
	}
}
