package uk.gov.ons.bulk.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

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
	private static String JOB_QUERY = "SELECT * FROM bulkinfo WHERE jobid = ?";
	private static String ALL_JOBS_QUERY = "SELECT * FROM bulkinfo WHERE userid like ? AND status like ?";

	@Autowired
	public BulkStatusRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Long saveJob(BulkInfo job) {
		
		simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate);
		simpleJdbcInsert.withTableName("bulkinfo")
			.usingGeneratedKeyColumns("jobid")
			.usingColumns("userid", "status", "totalrecs", "recssofar");
		Number id = simpleJdbcInsert.executeAndReturnKey(new BeanPropertySqlParameterSource(job));

		return id.longValue();
	}
	
	public List<BulkInfo> queryJob(long jobId) {
		return jdbcTemplate.query(JOB_QUERY, new BulkInfoMapper(), jobId);
	}

	public List<BulkInfo> getJobs(String userid, String status) {
		String userpattern = userid + '%';
		String statuspattern = status + '%';
		return jdbcTemplate.query(ALL_JOBS_QUERY, new BulkInfoMapper(), userpattern, statuspattern);
	}

	public class BulkInfoMapper implements RowMapper<BulkInfo> {

		@Override
		public BulkInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
			
			BulkInfo bulkInfo = new BulkInfo();

			Long correctedRecs = rs.getLong("recssofar");
			
			if (rs.getString("status").equals("finished"))
			{
				correctedRecs = rs.getLong("totalrecs");
			}
			
			bulkInfo.setJobid(rs.getLong("jobid"));
			bulkInfo.setUserid(rs.getString("userid"));
			bulkInfo.setStatus(rs.getString("status"));
			bulkInfo.setTotalrecs(rs.getLong("totalrecs"));
			bulkInfo.setRecssofar(correctedRecs);
			bulkInfo.setStartdate(rs.getTimestamp("startdate").toLocalDateTime());
			
			Timestamp endDateTimestamp = rs.getTimestamp("enddate");
			
			if (endDateTimestamp != null) {
				bulkInfo.setEnddate(rs.getTimestamp("enddate").toLocalDateTime());
			}
			
			return bulkInfo;
		}
	}
}
