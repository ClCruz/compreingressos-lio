package br.compreingressos.checkcompre.dao;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;

import br.compreingressos.checkcompre.model.Espetaculo;

/**
 * Created by luiszacheu on 15/04/15.
 */
public class EspetaculoDao extends BaseDaoImpl<Espetaculo, Integer> {

   public EspetaculoDao(ConnectionSource connectionSource) throws SQLException {
       super(Espetaculo.class);
       setConnectionSource(connectionSource);
       initialize();
   }
}
