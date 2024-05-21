package Controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import javax.servlet.ServletException;
import javax.servlet.RequestDispatcher;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Part;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.io.PrintWriter;

import model.ProductModel;
import model.game;

/**
 * Servlet implementation class AddGame
 */
@WebServlet("/AddGame")
@MultipartConfig()
public class UploadGame extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String SAVE_DIR = "uploads";
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "gif"));
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

	static ProductModel GameModels = new ProductModelDM();
	
	DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	LocalDateTime now = LocalDateTime.now();
	
	
    public UploadGame() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		PrintWriter out = response.getWriter();
		response.setContentType("text/plain");

		out.write("Error: GET method is used but POST method is required");
		out.close();
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	   protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	        Collection<?> games = (Collection<?>) request.getSession().getAttribute("games");
	        String savePath = request.getServletContext().getRealPath("") + File.separator + SAVE_DIR;

	        // Create directory if it does not exist
	        File fileSaveDir = new File(savePath);
	        if (!fileSaveDir.exists()) {
	            fileSaveDir.mkdir();
	        }

	        game g1 = new game();
	        String fileName = null;
	        String message = "upload =\n";
	        boolean isValid = true;
	        String errorMessage = "";

	        if (request.getParts() != null && request.getParts().size() > 0) {
	            for (Part part : request.getParts()) {
	                fileName = extractFileName(part);
	                if (fileName != null && !fileName.isEmpty()) {
	                    // Sanitize file name
	                    fileName = new File(fileName).getName();

	                    // Get file extension
	                    String fileExtension = getFileExtension(fileName).toLowerCase();
	                    
	                    if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
	                        isValid = false;
	                        errorMessage = "Invalid file type.";
	                        break;
	                    }

	                    // Check file size
	                    if (part.getSize() > MAX_FILE_SIZE) {
	                        isValid = false;
	                        errorMessage = "File is too large.";
	                        break;
	                    }

	                    // Check file content type
	                    if (!isValidContentType(part, fileExtension)) {
	                        isValid = false;
	                        errorMessage = "Invalid file content.";
	                        break;
	                    }

	                    if (isValid) {
	                        part.write(savePath + File.separator + fileName);
	                        g1.setImg(fileName);
	                        message += fileName + "\n";
	                    } else {
	                        request.setAttribute("error", errorMessage);
	                    }
	                } else {
	                    request.setAttribute("error", "Errore: Bisogna selezionare almeno un file");
	                }
	            }
	        }

	        // Set other game properties
	        LocalDateTime now = LocalDateTime.now();

	        g1.setName(request.getParameter("nomeGame"));
	        g1.setYears(request.getParameter("years"));
	        g1.setAdded(dtf.format(now));
	        g1.setQuantity(Integer.valueOf(request.getParameter("quantita")));
	        g1.setPEG(Integer.valueOf(request.getParameter("PEG")));
	        g1.setIva(Integer.valueOf(request.getParameter("iva")));
	        g1.setGenere(request.getParameter("genere"));
	        g1.setDesc(request.getParameter("desc"));
	        g1.setPrice(Float.valueOf(request.getParameter("price")));

	        try {
	            GameModels.doSave(g1);
	        } catch (SQLException e) {
	            e.printStackTrace();
	        }

	        request.setAttribute("message", message);
	        request.setAttribute("stato", "success!");

	        RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/gameList?page=admin&sort=added DESC");
	        dispatcher.forward(request, response);
	    }

	    private String extractFileName(Part part) {
	        String contentDisp = part.getHeader("content-disposition");
	        String[] items = contentDisp.split(";");
	        for (String s : items) {
	            if (s.trim().startsWith("filename")) {
	                return s.substring(s.indexOf("=") + 2, s.length() - 1);
	            }
	        }
	        return "";
	    }

	    private String getFileExtension(String fileName) {
	        int lastIndexOf = fileName.lastIndexOf(".");
	        return lastIndexOf == -1 ? "" : fileName.substring(lastIndexOf + 1);
	    }

	    private boolean isValidContentType(Part part, String fileExtension) throws IOException {
	        try (InputStream input = part.getInputStream()) {
	            String mimeType = Files.probeContentType(new File(part.getSubmittedFileName()).toPath());
	            switch (fileExtension) {
	                case "jpg":
	                case "jpeg":
	                    return mimeType.equals("image/jpeg");
	                case "png":
	                    return mimeType.equals("image/png");
	                case "gif":
	                    return mimeType.equals("image/gif");
	                default:
	                    return false;
	            }
	        }
	    }
}