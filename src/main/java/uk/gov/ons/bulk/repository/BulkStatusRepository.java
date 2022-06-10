package uk.gov.ons.bulk.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import uk.gov.ons.bulk.entities.BulkInfo;

@Repository
public class BulkStatusRepository {

	private JdbcTemplate jdbcTemplate;
	private SimpleJdbcInsert simpleJdbcInsert;
	private static String JOB_QUERY = "SELECT * FROM bulkinfo WHERE runid = ?";

	@Autowired
	public BulkStatusRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
		this.simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate);
	}

	public Long saveJob(BulkInfo job) {

		simpleJdbcInsert.withTableName("bulkinfo")
			.usingGeneratedKeyColumns("runid")
			.usingColumns("userid", "status", "totalrecs", "recssofar");
		Number id = simpleJdbcInsert.executeAndReturnKey(new BeanPropertySqlParameterSource(job));

		return id.longValue();
	}
	
	public BulkInfo queryJob(long jobId) {
		return jdbcTemplate.queryForObject(JOB_QUERY, new BulkInfoMapper(), jobId);
	}
	
	public class BulkInfoMapper implements RowMapper<BulkInfo> {

		@Override
		public BulkInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
			
			BulkInfo bulkInfo = new BulkInfo();
			
			bulkInfo.setRunid(rs.getLong("runid"));
			bulkInfo.setUserid(rs.getString("userid"));
			bulkInfo.setStatus(rs.getString("status"));
			bulkInfo.setTotalrecs(rs.getLong("totalrecs"));
			bulkInfo.setRecssofar(rs.getLong("recssofar"));
			bulkInfo.setStartdate(rs.getTimestamp("startdate").toLocalDateTime());
			
			Timestamp endDateTimestamp = rs.getTimestamp("enddate");
			
			if (endDateTimestamp != null) {
				bulkInfo.setEnddate(rs.getTimestamp("enddate").toLocalDateTime());
			}
			
			return bulkInfo;
		}
	}
}
