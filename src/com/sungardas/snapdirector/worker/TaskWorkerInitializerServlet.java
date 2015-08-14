package com.sungardas.snapdirector.worker;


import com.amazonaws.auth.AWSCredentialsProvider;
import com.sungardas.snapdirector.aws.EnvironmentBasedCredentialsProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import java.util.concurrent.ExecutorService;

/**
 * Servlet implementation class TaskWorkerInitializerServlet
 */
@WebServlet(
		loadOnStartup=2,
		urlPatterns = { "/TaskWorkerInitializerServlet" }, 
		initParams = { 
				@WebInitParam(name = "shutdown-on-unload", value = "true")
		})
public class TaskWorkerInitializerServlet extends HttpServlet {
	public static final Log LOG = LogFactory.getLog(TaskWorkerInitializerServlet.class);
	
	private static final long serialVersionUID = 1L;
	ExecutorService exec;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public TaskWorkerInitializerServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see Servlet#init(ServletConfig)
	 */
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		LOG.info("TaskWorkerInitializerServlet started...");
		ServletContext context = getServletContext();
		
		String credentials = context.getInitParameter("aws:credentials-file");
		String queueURL = context.getInitParameter("aws:sqs-queue-url");
		String routineInstanceId = context.getInitParameter("aws:routine-inst-id");
		String propertyFile = context.getInitParameter("tmp:sdfsbackup-properties");
		AWSCredentialsProvider credentialsProvider = new EnvironmentBasedCredentialsProvider();
		
//		exec = Executors.newSingleThreadExecutor();
//		exec.execute(new AWSTaskWorker(credentialsProvider,queueURL, routineInstanceId));
	}

	/**
	 * @see Servlet#destroy()
	 */
	public void destroy() {
		LOG.info("TaskWorkerInitializerServlet stoped.");
//		exec.shutdownNow();
	}

}
