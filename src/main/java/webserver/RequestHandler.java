package webserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import model.DataBase;
import model.User;
import util.HttpRequestUtils;
import util.IOUtils;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
        	// 요구사항1 : index.html 응답하기
        	
        	// 1단계 : InputStream을 한줄단위로 읽기위해 BufferedReader를 생성
        	BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        	String line = br.readLine();
        	log.debug(line);
        	if (line == null) {
        		return;
        	}
        	
        	
        	// 2단계 : http 요청 정보의 첫번쨰 라인에서 요청url 추출
        	String url = HttpRequestUtils.getUrl(line);
        	int length = 0;
        	while (!"".equals(line)) {
        		line = br.readLine();
        		log.debug(line);
        		if (line.contains("Content-Length")){
        			String str[] = line.split(":");
        			length = Integer.parseInt(str[1].trim());
        		}
        	}
        	
        	if (url.startsWith("/user/create")) {
        	
            	Map<String, String> userMap = HttpRequestUtils.parseQueryString(IOUtils.readData(br, length));
            	User user = new User(userMap.get("userId"), userMap.get("password"), userMap.get("name"), userMap.get("email"));
            	log.debug("user : {}" , user);
            	DataBase.addUser(user);
                // 3단계 : 요청 url에 해당하는 파일을 webapp디렉토리에서 읽어 전달
            	DataOutputStream dos = new DataOutputStream(out);
                response302Header(dos);
                
        	} else if (url.equals("/user/login")) {
        	
            	Map<String, String> userMap = HttpRequestUtils.parseQueryString(IOUtils.readData(br, length));
            	User user = DataBase.getUser(userMap.get("userId"));
            	
            	if ( user== null ) {
            		log.debug("user null");
            		 // 3단계 : 요청 url에 해당하는 파일을 webapp디렉토리에서 읽어 전달
                	DataOutputStream dos = new DataOutputStream(out);
                    response302Header(dos);
            	} else if (userMap.get("password").equals(user.getPassword())) {
            		log.debug("login success");
            		 // 3단계 : 요청 url에 해당하는 파일을 webapp디렉토리에서 읽어 전달
                	DataOutputStream dos = new DataOutputStream(out);
                	response302HeaderWithCookies(dos, "logined=true");
            	} else {
            		log.debug("password not true");
            		 // 3단계 : 요청 url에 해당하는 파일을 webapp디렉토리에서 읽어 전달
                	DataOutputStream dos = new DataOutputStream(out);
                    response302Header(dos);
            	}
            	log.debug("user : {}" , user);
                // 3단계 : 요청 url에 해당하는 파일을 webapp디렉토리에서 읽어 전달
            	DataOutputStream dos = new DataOutputStream(out);
                response302Header(dos);
                
        	} else if (url.endsWith(".css")) {
            	DataOutputStream dos = new DataOutputStream(out);
                byte[] body = Files.readAllBytes(new File("./webapp"+url).toPath());
                response200HeaderWithCss(dos, body.length);
                responseBody(dos, body);        		
        	} else {
            	DataOutputStream dos = new DataOutputStream(out);
                byte[] body = Files.readAllBytes(new File("./webapp"+url).toPath());
                response200Header(dos, body.length);
                responseBody(dos, body);
        	}
        	
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
    
    private void response200HeaderWithCss(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/css;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
    
    private void response302HeaderWithCookies(DataOutputStream dos, String strCookie) {
        try {
            dos.writeBytes("HTTP/1.1 302 OK \r\n");
            dos.writeBytes("Location: /index.html\r\n");
            dos.writeBytes("Set-Cookie: " + strCookie + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
    
    private void response302Header(DataOutputStream dos) {
        try {
            dos.writeBytes("HTTP/1.1 302 OK \r\n");
            dos.writeBytes("Location: /index.html\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
