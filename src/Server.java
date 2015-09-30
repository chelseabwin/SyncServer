import java.io.*;  
import java.net.ServerSocket;  
import java.net.Socket;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
 
public class Server {
	private int listenPort; // 端口号
	
	// 构造器
	public Server(int listenPort) throws IOException {
        this.listenPort = listenPort;
	}
	
	// 开始侦听   
    public void start() {
        new ListenThread().start();
    }
    
    /**
	 *  监听线程
	 */
    private class ListenThread extends Thread {
    	private ServerSocket server;
        @Override
        public void run() {
            try {
                server = new ServerSocket(listenPort);
                // 开始循环
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
	 *  数据操作线程
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
            	// 获得输入流
                is = socket.getInputStream();
                br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                // 获得输出流
                os = socket.getOutputStream();
                pw = new PrintWriter(os);
                // 读取用户输入信息
                String info = null;
                while(!((info = br.readLine()) == null)) {
                    //System.out.println("json串："+info);
                    analysisJson(info); // json串解析及数据库操作
                }
                // 返回客户端一个响应
                String reply = "finish";
                pw.write(reply);
                pw.flush();                
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                	// 关闭资源
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
    
    /** json串提取解析
	 * 
	 * @param json 待解析json串
	 */
    public void analysisJson(String json) {    	
    	try {
			JSONArray jsonDb = new JSONArray(json);
			for (int i = 0; i < jsonDb.length(); i++) {
				JSONObject jsonTable = jsonDb.getJSONObject(i); // 获取表数据
				
				@SuppressWarnings("unchecked")
				Iterator<String> itTable = jsonTable.keys();
				while (itTable.hasNext()) {
		            String tableName = itTable.next().toString();
		            JSONArray jsonRow = jsonTable.getJSONArray(tableName); // 获取行数据集
		            
		            for (int j = 0; j < jsonRow.length(); j++) {
		            	JSONObject jsonField = jsonRow.getJSONObject(j); // 获取某一行数据
		            	String flag = null;
		            	if (jsonField.has("flag")) {
		            		flag = jsonField.getString("flag");
		            		
		            		@SuppressWarnings("unchecked")
							Iterator<String> itField = jsonField.keys();
			            	System.out.println(jsonField.toString());
			            	
			            	StringBuffer fieldkey = new StringBuffer(); // 用于添加，存储字段名称
	            	    	StringBuffer fieldValue = new StringBuffer(); // 用于添加，存储字段值
	            	    	StringBuffer fieldSetValue = new StringBuffer(); // 用于修改，存储字段名和值
	            	    	
			            	while (itField.hasNext()) {
			            		String key = itField.next().toString();
			            		if (flag.equals("0")) { // 添加			            	    	
				            		fieldkey.append(key);
				            		if (itField.hasNext())
				            			fieldkey.append(",");
				            		
				            		fieldValue.append("'" + jsonField.getString(key) + "'");
				            		if (itField.hasNext())
				            			fieldValue.append(",");
								}
			            		else if (flag.equals("2")) { // 修改
			            			fieldSetValue.append(key + "='" + jsonField.getString(key) + "'");
			            			if (itField.hasNext())
			            				fieldSetValue.append(",");
								}
			            	}
			            	
			            	if (flag.equals("0")){ // 添加
			            		insertData(tableName, fieldkey.toString(), fieldValue.toString());			            		
			            	}
			            	else if (flag.equals("2")) { // 修改
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
    
    /** 插入数据
	 * 
	 * @param table 待插入表名
	 * @param key 插入参数名
	 * @param values 插入参数值
	 * @return 插入成功返回新数据id，失败返回0
	 */
    public int insertData(String table, String key, String values) {
		String sql = "insert into " + table + "(" + key + ") values(" + values + ")";
		System.err.println(sql);
		return 0;
	}
    
    /** 修改数据
	 * 
	 * @param table 待修改表名
	 * @param setValue 修改参数
	 * @param where 修改位置
	 * @return 修改成功返回1，失败返回0
	 */
    public int updateData(String table, String setValue, String where) {
		String sql = null;
		if (where == null) {
			sql = "update " + table + " set " + setValue;
		}
		else {
			sql = "update " + table + " set " + setValue + " where " + where;
		}
		System.err.println(sql);
		return 0;
	}
	
	public static void main(String[] args) throws Exception {
		//new Server(30000).start();
		String str1 = "[{\"base1\":[{\"across_type\":\"[1]沟壑\",\"custody_unit\":\"\",\"rode_grade\":\"[0]高速公路\",\"location\":\"\",\"flag\":\"0\"}]},"
				+ "{\"base2\":[]},{\"base3\":[]},{\"support_detail\":[]}]";
		new Server(0).analysisJson(str1);
	}
}

