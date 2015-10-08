import java.io.*;  
import java.net.ServerSocket;  
import java.net.Socket;
import java.sql.SQLException;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
 
public class Server {
	private int listenPort; // �˿ں�
	
	// ������
	public Server(int listenPort) throws IOException {
        this.listenPort = listenPort;
	}
	
	// ��ʼ����   
    public void start() {
        new ListenThread().start();
    }
    
    /**
	 *  �����߳�
	 */
    private class ListenThread extends Thread {
    	private ServerSocket server;
        @Override
        public void run() {
            try {
                server = new ServerSocket(listenPort);
                // ��ʼѭ��
                while (true) {
                    Socket socket = server.accept();
                    new HandleThread(socket).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
            	try {
					server.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
        }
    }
    
    /**
	 *  ���ݲ����߳�
	 */
    private class HandleThread extends Thread {
        private Socket socket;
        private InputStream is;
        private BufferedReader br;
        private OutputStream os;
        private PrintWriter pw;
        
        private HandleThread(Socket socket) {
            this.socket = socket;
        }
   
        @Override
        public void run() {
            try {
            	// ���������
                is = socket.getInputStream();
                br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                // ��������
                os = socket.getOutputStream();
                pw = new PrintWriter(os);
                // ��ȡ�û�������Ϣ
                String info = null;
                while(!((info = br.readLine()) == null)) {
                    analysisJson(info); // json�����������ݿ����
                }
                // ���ؿͻ���һ����Ӧ
                String reply = "finish";
                pw.write(reply);
                pw.flush();                
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                	// �ر���Դ
                    pw.close();
                    os.close();
                    br.close();
                    is.close();
                    socket.close();
                } catch (IOException e) {
                	// TODO Auto-generated catch block
					e.printStackTrace();
                }
            }
        }
    }
    
    /** json����ȡ����
	 * 
	 * @param json ������json��
	 */
    public void analysisJson(String json) {    	
    	try {
			JSONArray jsonDb = new JSONArray(json);
			for (int i = 0; i < jsonDb.length(); i++) {
				JSONObject jsonTable = jsonDb.getJSONObject(i); // ��ȡ������
				
				@SuppressWarnings("unchecked")
				Iterator<String> itTable = jsonTable.keys();
				while (itTable.hasNext()) {
		            String tableName = itTable.next().toString();
		            JSONArray jsonRow = jsonTable.getJSONArray(tableName); // ��ȡ�����ݼ�
		            
		            for (int j = 0; j < jsonRow.length(); j++) {
		            	JSONObject jsonField = jsonRow.getJSONObject(j); // ��ȡĳһ������
		            	String flag = null;
		            	if (jsonField.has("flag")) {
		            		flag = jsonField.getString("flag");
		            		
		            		@SuppressWarnings("unchecked")
							Iterator<String> itField = jsonField.keys();
			            	
			            	StringBuffer fieldkey = new StringBuffer(); // ������ӣ��洢�ֶ�����
	            	    	StringBuffer fieldValue = new StringBuffer(); // ������ӣ��洢�ֶ�ֵ
	            	    	StringBuffer fieldSetValue = new StringBuffer(); // �����޸ģ��洢�ֶ�����ֵ
	            	    	
			            	while (itField.hasNext()) {
			            		String key = itField.next().toString();
			            		if (flag.equals("0")) { // ���			            	    	
				            		fieldkey.append(key);
				            		if (itField.hasNext())
				            			fieldkey.append(",");
				            		
				            		fieldValue.append("'" + jsonField.getString(key) + "'");
				            		if (itField.hasNext())
				            			fieldValue.append(",");
								}
			            		else if (flag.equals("2")) { // �޸�
			            			fieldSetValue.append(key + "='" + jsonField.getString(key) + "'");
			            			if (itField.hasNext())
			            				fieldSetValue.append(",");
								}
			            	}
			            	
			            	if (flag.equals("0")){ // ���
			            		insertData(tableName, fieldkey.toString(), fieldValue.toString());			            		
			            	}
			            	else if (flag.equals("2")) { // �޸�
			            		updateData(tableName, fieldSetValue.toString(), null);
							}
						}
					}
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    /** ��������
	 * 
	 * @param table ���������
	 * @param key ���������
	 * @param values �������ֵ
	 */
    public void insertData(String table, String key, String values) {
		String sql = "insert into " + table + "(" + key + ") values(" + values + ")";
		
		DBHelper db = new DBHelper(sql); //����DBHelper����
		try {
			db.pst.executeUpdate(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
    
    /** �޸�����
	 * 
	 * @param table ���޸ı���
	 * @param setValue �޸Ĳ���
	 * @param where �޸�λ��
	 */
    public void updateData(String table, String setValue, String where) {
		String sql = null;
		if (where == null) {
			sql = "update " + table + " set " + setValue;
		}
		else {
			sql = "update " + table + " set " + setValue + " where " + where;
		}
		
		DBHelper db = new DBHelper(sql); //����DBHelper����
		try {
			db.pst.executeUpdate(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws Exception {
		new Server(30000).start();
	}
}

