package data.game.entry;

import data.io.DataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by LM on 02/03/2017.
 */
public class Serie {
    private int igdb_id = -1;
    private String name;
    private boolean id_needs_update;

    public Serie(int igdb_id, String name) {
        if(name == null || name.isEmpty()){
            throw new IllegalArgumentException("Serie's name was either null or empty : \""+name+"\"");
        }
        this.igdb_id = igdb_id;
        this.name = name;
    }

    //TODO implement a method to check if id_needs_update in db and do it if valid igdb_id. Same for Publisher and Developer

    public Serie(String name) {
        if(name == null || name.isEmpty()){
            throw new IllegalArgumentException("Serie's name was either null or empty : \""+name+"\"");
        }
        this.name = name;
    }

    public int insertInDB(){
        try {
            String sql = "INSERT OR IGNORE INTO Serie(name,"+ (igdb_id < 0 ? "id_needs_update) VALUES (?,?)" : "igdb_id) VALUES (?,?)");
            PreparedStatement serieStatement = DataBase.getUserConnection().prepareStatement(sql);
            serieStatement.setString(1, name);
            if(igdb_id >= 0){
                serieStatement.setInt(2,igdb_id);
            }else{
                serieStatement.setInt(2,1);
            }
            serieStatement.execute();
            serieStatement.close();
            //DataBase.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        igdb_id = getIdInDb();
        return igdb_id;
    }

    private int getIdInDb(){
        if(name == null || name.isEmpty()){
            throw new IllegalArgumentException("Serie's name was either null or empty : \""+name+"\"");
        }
        int id = -1;
        try {
            Connection connection = DataBase.getUserConnection();
            PreparedStatement getIdQuery = connection.prepareStatement("SELECT igdb_id FROM Serie WHERE name = ?");
            getIdQuery.setString(1,name);
            ResultSet result = getIdQuery.executeQuery();

            if(result.next()){
                id = result.getInt(1);
                result.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return id;
    }
}
