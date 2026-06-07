package dao;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class FaceEncodingDAO {
    private final Gson gson = new Gson();

    public List<float[]> getUserEncodings(int userId) {
        List<float[]> encodings = new ArrayList<>();
        String sql = "SELECT encoding_json FROM face_encodings WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String json = rs.getString("encoding_json");
                    List<List<Double>> list = gson.fromJson(json, new TypeToken<List<List<Double>>>(){}.getType());
                    if (list != null) {
                        for (List<Double> vec : list) {
                            float[] fVec = new float[vec.size()];
                            for (int i = 0; i < vec.size(); i++) {
                                fVec[i] = vec.get(i).floatValue();
                            }
                            encodings.add(fVec);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching encodings: " + e.getMessage());
        }
        return encodings;
    }

    public void saveUserEncodings(int userId, List<float[]> encodings) {
        String sql = "INSERT INTO face_encodings (user_id, encoding_json) VALUES (?, ?) ON DUPLICATE KEY UPDATE encoding_json = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            List<List<Float>> list = new ArrayList<>();
            for (float[] f : encodings) {
                List<Float> l = new ArrayList<>();
                for (float val : f) l.add(val);
                list.add(l);
            }
            String json = gson.toJson(list);
            
            ps.setInt(1, userId);
            ps.setString(2, json);
            ps.setString(3, json);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
