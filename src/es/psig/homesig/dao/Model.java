package es.psig.homesig.dao;

import java.awt.Color;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import es.psig.homesig.model.Links;
import es.psig.homesig.model.News;
import es.psig.homesig.model.Node;
import es.psig.homesig.util.Utils;


public class Model {
	
	private Connection conn = null;
	private ArrayList<Node> nodes;
	private ArrayList<News> news;
	private ArrayList<Links> links;
	private ArrayList<Node> currentPath;
	private static final String DB_PATH = "config/home_sig.sqlite";
	
	
	public Model() {
		
        Utils.getLogger();		
		if (!setConnection(DB_PATH)){
			System.exit(-1);
		}
		nodes = new ArrayList<Node>();
		news = new ArrayList<News>();
		links = new ArrayList<Links>();
		currentPath = new ArrayList<Node>();
		
	}

	
	public Connection getConn() {
		return conn;
	}

	
	public void setConn(Connection conn) {
		this.conn = conn;
	}


	/**
	  * Connecta a una base de dades amb la ruta passada com a argument
	  * Si el arxiu no existeix, el crea
	  * @param fileName - La ruta de la DB a connectar.
	  * @return La connexi� a la ruta.
	  */
    public boolean setConnection(String fileName) {

        try {
            Class.forName("org.sqlite.JDBC");
            File file = new File(fileName);
            if (file.exists()) {
            	conn = DriverManager.getConnection("jdbc:sqlite:" + fileName);
                return true;
            } else {
                Utils.showError("File not found", fileName, "Arbol");
                return false;
            }
        } catch (SQLException e) {
            Utils.showError("Database Error Connection", e.getMessage(), "Arbol");
            return false;
        } catch (ClassNotFoundException e) {
            Utils.showError("Database Error Connection", "ClassNotFoundException", "Arbol");
            return false;
        }

    }  
    
	
	public ArrayList<Node> getCurrentPath() {
		return currentPath;
	}

	public Node getCurrentParent() {
		int n = currentPath.size();
		if (n > 0) {
			return currentPath.get(n-1);
		}
		return null;
	}

	public void addToPath(Node n) {
		currentPath.add(n);
	}
	
	public void removeLastPathNode() {
		int n = currentPath.size();
		if (n > 0) {
			currentPath.remove(n-1);
		}
	}
	

	/** 
	 * Funci� principal. Llegim de la base de dades i guardem a mem�ria els nodes i les seves
	 * relacions paterno-filials
	*/
	public void createTree() {
		
		Statement stat = null;
		String sql = "select * from main;";
		try {
			stat = conn.createStatement();
			ResultSet rs = stat.executeQuery(sql);
		    while (rs.next()) {
		    	// llegim el fitxer i l'afegim a la nostra llista de fitxers
		    	Node n = new Node(rs.getString("id"),rs.getString("name"),rs.getString("link"),	rs.getString("tooltip"));
		    	if (n.getTooltip() == null) {
		    		n.setTooltip(n.getName());
		    	}
		    	nodes.add(n);
		    }
		    // Assignem el fill a la llista de fills del seu pare
		    for (int i=0; i < nodes.size(); ++i) {
		    	if (nodes.get(i).getLevel() > 1) {
		    		assignParent(nodes.get(i));
		    	}
		    }
		    // Ordenem els fills segons la posici�
		    sortChildren();
		    rs.close();
		} catch (SQLException e1) {
			Utils.showError(e1.getMessage(), sql, "");
		}
		
	}
	
	
	public void createNews() {	
		
		Statement stat = null;
		String sql = "select * from news ORDER BY date_act DESC;";
		try {
			stat = conn.createStatement();
			ResultSet rs = stat.executeQuery(sql);
		    while (rs.next()) {
		    	News n = new News(rs.getString("id"),rs.getString("title"),rs.getString("description"),rs.getString("link"));
		    	news.add(n);
		    }
		    rs.close();
		    conn.close();
		} catch (SQLException e1) {
			Utils.showError(e1.getMessage(), sql, "");
		}	
		
	}
	
	
	public void createLinks() {	
		
		Statement stat = null;
		String sql = "select * from links;";
		try {
			stat = conn.createStatement();
			ResultSet rs = stat.executeQuery(sql);
		    while (rs.next()) {
		    	Links n = new Links(rs.getString("id"),rs.getString("title"),rs.getString("path"),rs.getString("image"));
		    	links.add(n);
		    }
		    rs.close();
		    //conn.close();
		} catch (SQLException e1) {
			Utils.showError(e1.getMessage(), sql, "");
		}	
		
	}
	
	
	/**
	 * Ordena els fills segons la posici� que han de tenir
	 */
	private void sortChildren() {
		Collections.sort(nodes, new NodeComparator());
		for (int i=0; i < nodes.size(); ++i) {
			Collections.sort(nodes.get(i).getChildren(), new NodeComparator());
		}
	}

	
	/** 
	 * Classe que fa el comparador entre nodes
	 * @author Roger Erill
	 *
	 */
	public class NodeComparator implements Comparator<Node> {
		@Override
		public int compare(Node n1, Node n2) {
			Integer id1 = Integer.valueOf(n1.getPosition());
			Integer id2 = Integer.valueOf(n2.getPosition());
			return id1.compareTo(id2);
		}
	}
	

	/**
	 * 
	 * @return Una llista de nodes que formen part del primer nivell de l'arbre
	 */
	public ArrayList<Node> getFirstLevel() {
		ArrayList<Node> result = new ArrayList<Node>();
		for (int i=0; i < nodes.size(); ++i) {
			if (nodes.get(i).getLevel() == 1) {
				result.add(nodes.get(i));
			}
		}
		return result;
	}
	
	
	/**
	  * Donat un node children, busquem el seu pare i li assignem com a pare
	  * i al seu pare com a fill
	  * @param children - El node al qui busquem el pare per assignar-li com a fill.
	  */ 
	private void assignParent(Node children) {
		for (int i=0; i < nodes.size(); ++i) {
			if (nodes.get(i).getId().equals(children.getParent_id()) ) {
				nodes.get(i).addChildren(children.getPosition(), children);
				children.setParent(nodes.get(i));
			}
		}
	}
	
	
	public ArrayList<Node> getAll() {
		return nodes;
	}
	
	
	/**
	 * 
	 * @param fileName nom del node
	 * @param pare nom del pare del fileName
	 * @return El node amb nom fileName
	 */
	public Node getNodeNamed(String fileName, String pare) {
		
		for (int i=0; i < nodes.size(); ++i) {
			if (nodes.get(i).getName().equals(fileName)) {
				if (nodes.get(i).getParent() != null && pare != null) {
					if (pare.equals(nodes.get(i).getParent().getName())) {
						return nodes.get(i);
					}
				}
				else if (nodes.get(i).getParent() == null && pare == null) {
					return nodes.get(i);
				}
				else {
					
				}
			}
		}
		return null;
		
	}
	
	
	/**
	 * 
	 * @param fileName nom del node
	 * @return El node amb nom fileName
	 */
	public Node getNodeDirectoryNamed(String fileName) {
		
		for (int i=0; i < nodes.size(); ++i) {
			if (nodes.get(i).getName().equals(fileName)) {
				return nodes.get(i);
			}
		}
		return null;
		
	}
	
	
	/**
	 * 
	 * @param link ruta del node
	 * @return El node amb ruta link
	 */
	public Node getNodeWithLink(String link) {
		
		for (int i=0; i < nodes.size(); ++i) {
			if (nodes.get(i).getLink() != null) {
				if (nodes.get(i).getLink().equals(link)) return nodes.get(i);
			}
		}
		return null;
		
	}
	
	
	/**
	 * 
	 * @param link ruta del node
	 * @return El node amb ruta link
	 */
	public Node getNodeWithId(String id) {
		
		for (int i=0; i < nodes.size(); ++i) {
			if (nodes.get(i).getId().equals(id)) {
				return nodes.get(i);
			}
		}
		return null;
		
	}

	
	/**
	 * 
	 * @return Array de Strings amb el nom dels nodes que formen part del current path
	 */
	public String[] drawPath() {
		
		String[] res = new String[currentPath.size()+1];
		res[0] = "Inici ";
		for (int i=0; i < currentPath.size(); ++i) {
			res[i+1] = currentPath.get(i).getName();
		}
		return res;
		
	}


	/**
	 * Donat un nom de node, creem el path on estem actualment
	 * @param text Nom del node a partir del qui crearem la ruta
	 */
	public void createPathOf(String text, String pare) {
		Node n = getNodeNamed(text, pare);
		currentPath.clear();
		createPathRecursively(n);
	}


	private void createPathRecursively(Node n) {
		if (n.getLevel() == 1) {
			currentPath.add(n);
		}
		else {
			createPathRecursively(n.getParent());
			currentPath.add(n);
		}
	}


	public String[] drawEmptyPath() {
		currentPath.clear();
		String[] res = new String[1];
		res[0] = "Inici ";
		return res;
	}

	
	public ArrayList<String> createHtlm() {
		
		ArrayList<String> res = new ArrayList<String>();
		for (int i=0; i < news.size(); ++i) {
			News n = news.get(i);
			Node linked = getNodeWithId(n.getLink());
			String link = "";
			try {
				link = linked.getLink();
			}
			catch (NullPointerException e) {
				link = "";
				//Utils.getLogger().info("El link " + n.getLink() + " de la not�cia " + n.getTitle() + " no existeix");
			}
			String htmlNews = null;
			if (link == null || link.isEmpty()) {
				htmlNews = "<b>" + n.getTitle() + "</b><br>";
			}
			else {
				htmlNews = "<b><a href='" + link + "'> " + n.getTitle() + " </a></b><br>";
			}
			htmlNews += n.getDesc();
			res.add(htmlNews);
		}
		return res;
		
	}
	
	
	public ArrayList<Links> createLinksHtlm() {
		
		ArrayList<Links> res = new ArrayList<Links>();
		for (int i=0; i < links.size(); ++i) {
			Links l = links.get(i);
			String htmlLinks = "<a href='" + l.getPath() + "'> " + l.getName() + " </a><br>";
			links.get(i).setHtmlcode(htmlLinks);
			res.add(links.get(i));
		}
		return res;
		
	}
	

	private String getValueOf(String field) {
		
		Statement stat = null;
		String sql = "select value from properties where field = \"" + field + "\"";
		try {
			stat = conn.createStatement();
			ResultSet rs = stat.executeQuery(sql);
			if (rs.next()) {
				return rs.getString("value");
			}
		}
		catch (SQLException e) {
			Utils.showError(e.getMessage(), sql, "");
		}
		return "ERROR";
		
	}
	
	
	public String getUpperLogoPath() {
		return getValueOf("upperLogo");
	}
	
	public String getTitleIcon() {
		return getValueOf("titleIcon");
	}
	
	private Color createColor(String s) {
		String[] rgb = s.split(",");
		Float red = Float.valueOf(rgb[0])/255f;
		Float green = Float.valueOf(rgb[1])/255f;
		Float blue = Float.valueOf(rgb[2])/255f;
		Color res = new Color(red,green,blue);
		return res;
	}
	
	public Color getBackground() {
		return createColor(getValueOf("backgroundColor"));
	}
	
	public Color getTitlesForeground() {
		return createColor(getValueOf("titlesColor"));
	}
	
	public Color getDarkGrey() {
		return createColor(getValueOf("backFillAriadnaColor"));
	}
	
	public Color getLightGrey() {
		return createColor(getValueOf("backTextBoxColor"));
	}
	
	public Color getIniciForeground() {
		return createColor(getValueOf("iniciFontColor"));
	}
	
	public Color getBreadcrumbForeground() {
		return createColor(getValueOf("breadcrumbFontColor"));
	}

	public String getTitle() {
		return getValueOf("title");
	}
	
	public String getWindowTitle() {
		return getValueOf("windowTitle");
	}
	
	public String getSubtitle() {
		return getValueOf("subtitle");
	}
	
	public String getAddress() {
		return getValueOf("address");
	}
	
	public String getTelephone() {
		return getValueOf("telephone");
	}

	public String getFax() {
		return getValueOf("fax");
	}
	
	public String getEmail() {
		return getValueOf("email");
	}
	
	public String getConsultor() {
		return getValueOf("consultor");
	}
	
	public String getWebDesign() {
		return getValueOf("webDesign");
	}
	
	
}