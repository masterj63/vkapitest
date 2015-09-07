package mj.vkapitest;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.view.Camera;
import org.graphstream.ui.view.Viewer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseWheelEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class Main {
    private static final String NAME_REQUEST_URL = "https://api.vk.com/method/users.get?user_id=";
    private static final String FRIENDS_REQUEST_URL = "https://api.vk.com/method/friends.get?user_id=";

    private static long MY_ID;
    private static long watch;

    private static JSONArray request(String url) throws IOException {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        //add request header
        con.setRequestProperty("User-Agent", "Mozilla/5.0");

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer responseSB = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            responseSB.append(inputLine);
        }
        in.close();

        String response = responseSB.toString();
        JSONObject jsonObject = new JSONObject(response);
        if (!jsonObject.has("response"))
            return null;
        JSONArray jsonArray = (JSONArray) jsonObject.get("response");
        return jsonArray;
    }

    private static String getName(long id) throws IOException {
        JSONArray jsonArray = request(NAME_REQUEST_URL + id);
        if (jsonArray == null)
            return "???";
        JSONObject jsonObject = jsonArray.getJSONObject(0);
        String name = jsonObject.getString("last_name");
        return cpToUtf(name);
    }

    private static ArrayList<Long> getFriendsList(long id) throws IOException {
        JSONArray jsonArray = request(FRIENDS_REQUEST_URL + id);
        if (jsonArray == null)
            return new ArrayList<>();

        ArrayList<Long> res = new ArrayList<>(jsonArray.length());
        for (int i = 0; i < jsonArray.length(); i++)
            res.add(jsonArray.getLong(i));
        Collections.shuffle(res);

        return res;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length == 1)
            MY_ID = Long.parseLong(args[0]);
        else {
            System.out.println("Give me an ID!");
            return;
        }

        startWatch();

        Graph graph = new MultiGraph("I am graph");
        String styleSheet = "node { fill-color: black; }" +
                "node.marked { fill-color: red; }" +
                "edge { fill-color: red; }";
        graph.addAttribute("ui.stylesheet", styleSheet);
        graph.setAutoCreate(true);
        graph.setStrict(false);
        Viewer viewer = graph.display();
        final Camera camera = viewer.getDefaultView().getCamera();

        viewer.getDefaultView().addMouseWheelListener(new MouseAdapter() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                double z = 0.0;
                if (e.getUnitsToScroll() > 0)
                    z = 0.12;
                if (e.getUnitsToScroll() < 0) {
                    z = -0.12;
                    while (camera.getViewPercent() + z <= 0.0d)
                        z /= 2;
                }
                camera.setViewPercent(camera.getViewPercent() + z);
            }
        });
        viewer.getDefaultView().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                double dd = 0.35d, dx = 0.0d, dy = 0.0d, dz = 0.0d;

                if (e.getKeyCode() == KeyEvent.VK_W)
                    dy = +dd;
                else if (e.getKeyCode() == KeyEvent.VK_S)
                    dy = -dd;

                if (e.getKeyCode() == KeyEvent.VK_D)
                    dx = +dd;
                else if (e.getKeyCode() == KeyEvent.VK_A)
                    dx = -dd;

                Point3 p3 = camera.getViewCenter();
                camera.setViewCenter(p3.x + dx, p3.y + dy, p3.z + dz);
            }
        });


        List<Long> myFriendsIdsList = getFriendsList(MY_ID);
        final int N = myFriendsIdsList.size();

        Map<Long, Integer> idIndexMap = new HashMap<>();
        for (int i = 0; i < N; i++)
            idIndexMap.put(myFriendsIdsList.get(i), i);

        for (int myFriendsIndex = 0; myFriendsIndex < N; myFriendsIndex++) {
            Long myFriendsId = myFriendsIdsList.get(myFriendsIndex);
            String id0 = String.valueOf(myFriendsIdsList.get(myFriendsIndex));
            for (long myFriendsFriendsId : getFriendsList(myFriendsId)) {
                Integer myFriendsFriendsIndex = idIndexMap.get(myFriendsFriendsId);
                if (myFriendsFriendsIndex == null)
                    continue;
                String id1 = String.valueOf(myFriendsIdsList.get(myFriendsFriendsIndex));

                String edgeId;
                if (myFriendsIndex < myFriendsFriendsIndex)
                    edgeId = id0 + "---" + id1;
                else
                    edgeId = id1 + "---" + id0;

                if (graph.getEdge(edgeId) != null)
                    continue;

                graph.addEdge(edgeId, id0, id1);
                //Thread.sleep(180);
            }
        }

//        for (Node node : graph)
//            node.addAttribute("layout.frozen");

        for (Node node : graph)
            node.addAttribute("ui.label", getName(Long.parseLong(node.getId())));

        stopWatch();
    }

    private static void startWatch() {
        watch = -System.currentTimeMillis();
    }

    private static void stopWatch() {
        watch += System.currentTimeMillis();
        System.out.printf("\nIt took %d seconds.\n", watch / 1000);
    }

    private static String cpToUtf(String cp) throws UnsupportedEncodingException {
        return new String(cp.getBytes("Cp1251"), "UTF-8");
    }
}
