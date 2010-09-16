package com.myapp.test;

import com.myapp.test.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;

public class MessageServlet extends HttpServlet {

	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory
			.getLogger(MessageServlet.class);

	private MessageRepository messageRepository = CachingMessageRepository.get();
	
	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		if (log.isDebugEnabled()) {
			log.debug("doGet");
		}

        String idString = request.getParameter("id");
        Long id = Long.parseLong(idString);
		
		Message message = messageRepository.getById(id);
        request.setAttribute("message", message);

        forward(request, response, "message.jsp");
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
