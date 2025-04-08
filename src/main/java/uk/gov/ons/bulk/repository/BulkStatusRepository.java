package uk.gov.ons.bulk.repository;

import static uk.gov.ons.bulk.util.BulkServiceConstants.Status.PF;

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
import uk.gov.ons.bulk.entities.IdsBulkInfo;
import uk.gov.ons.bulk.util.BulkServiceConstants.Status;

@Repository
public class BulkStatusRepository {

	private JdbcTemplate jdbcTemplate;
	private SimpleJdbcInsert simpleJdbcInsert;
	private static String JOB_QUERY = "SELECT * FROM bulkinfo WHERE jobid = ?";
	private static String ALL_JOBS_QUERY = "SELECT * FROM bulkinfo WHERE userid like ? AND status like ?";
	
	private static String IDS_JOB_QUERY = "SELECT * FROM ids_bulkinfo WHERE jobid = ?";
	private static String IDS_ALL_JOBS_QUERY = "SELECT * FROM ids_bulkinfo WHERE userid like ? AND status like ?";
	private static String IDS_SINGLE_JOB_QUERY = "SELECT * FROM ids_bulkinfo WHERE idsjobid = ?";
	private static String IDS_UPDATE_STATUS = "UPDATE ids_bulkinfo SET status = ? WHERE jobid = ?";

	@Autowired
	public BulkStatusRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Long saveJob(BulkInfo job) {
		
		simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate);
		simpleJdbcInsert.withTableName("bulkinfo")
			.usingGeneratedKeyColumns("jobid")
			.usingColumns("userid", "status", "totalrecs", "recssofar", "dataset", "topic", "uimetadata");
		Number id = simpleJdbcInsert.executeAndReturnKey(new BeanPropertySqlParameterSource(job));

		return id.longValue();
	}
	
	public Long saveIdsJob(IdsBulkInfo idsJob) {
		
		simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate);
		simpleJdbcInsert.withTableName("ids_bulkinfo")
			.usingGeneratedKeyColumns("jobid")
			.usingColumns("idsjobid", "userid", "status", "totalrecs", "recssofar", "test", "dataset", "topic", "uimetadata");
		Number id = simpleJdbcInsert.executeAndReturnKey(new BeanPropertySqlParameterSource(idsJob));

		return id.longValue();
	}
	
	public void updateStatus(long jobId, Status status) {
		
		jdbcTemplate.update(IDS_UPDATE_STATUS, preparedStatement -> {
			preparedStatement.setString(1, status.getStatus());
			preparedStatement.setLong(2, jobId);
		});
	}
	
	public List<BulkInfo> queryJob(long jobId) {
		return jdbcTemplate.query(JOB_QUERY, new BulkInfoMapper(), jobId);
	}
	
	public List<IdsBulkInfo> queryIdsJob(long jobId) {
		return jdbcTemplate.query(IDS_JOB_QUERY, new IdsBulkInfoMapper(), jobId);
	}

	public List<BulkInfo> getJobs(String userid, String status) {
		String userpattern = userid + '%';
		String statuspattern = status + '%';
		return jdbcTemplate.query(ALL_JOBS_QUERY, new BulkInfoMapper(), userpattern, statuspattern);
	}

	public List<IdsBulkInfo> getIdsJobs(String userid, String status) {
		String userpattern = userid + '%';
		String statuspattern = status + '%';
		return jdbcTemplate.query(IDS_ALL_JOBS_QUERY, new IdsBulkInfoMapper(), userpattern, statuspattern);
	}
	
	public List<IdsBulkInfo> getIdsJob(String idsJobId) {
		return jdbcTemplate.query(IDS_SINGLE_JOB_QUERY, new IdsBulkInfoMapper(), idsJobId);
	}
	
	public class BulkInfoMapper implements RowMapper<BulkInfo> {

		@Override
		public BulkInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
			
			BulkInfo bulkInfo = new BulkInfo();

			Long correctedRecs = rs.getLong("recssofar");
			
			if (rs.getString("status").equals(PF.getStatus()))
			{
				correctedRecs = rs.getLong("totalrecs");
			}
			
			bulkInfo.setJobid(rs.getLong("jobid"));
			bulkInfo.setUserid(rs.getString("userid"));
			bulkInfo.setStatus(rs.getString("status"));
			bulkInfo.setTotalrecs(rs.getLong("totalrecs"));
			bulkInfo.setRecssofar(correctedRecs);
			bulkInfo.setDataset(rs.getString("dataset"));
			bulkInfo.setTopic(rs.getString("topic"));
			bulkInfo.setUimetadata(rs.getString("uimetadata"));
			bulkInfo.setStartdate(rs.getTimestamp("startdate").toLocalDateTime());
			
			Timestamp endDateTimestamp = rs.getTimestamp("enddate");
			
			if (endDateTimestamp != null) {
				bulkInfo.setEnddate(rs.getTimestamp("enddate").toLocalDateTime());
			}
			
			return bulkInfo;
		}
	}

	public class IdsBulkInfoMapper implements RowMapper<IdsBulkInfo> {

		@Override
		public IdsBulkInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
			
			IdsBulkInfo idsBulkInfo = new IdsBulkInfo();

			Long correctedRecs = rs.getLong("recssofar");
			
			if (rs.getString("status").equals(PF.getStatus()))
			{
				correctedRecs = rs.getLong("totalrecs");
			}
			
			idsBulkInfo.setJobid(rs.getLong("jobid"));
			idsBulkInfo.setIdsJobId(rs.getString("idsjobid"));
			idsBulkInfo.setUserid(rs.getString("userid"));
			idsBulkInfo.setStatus(rs.getString("status"));
			idsBulkInfo.setTotalrecs(rs.getLong("totalrecs"));
			idsBulkInfo.setRecssofar(correctedRecs);
			idsBulkInfo.setDataset(rs.getString("dataset"));
			idsBulkInfo.setTopic(rs.getString("topic"));
			idsBulkInfo.setUimetadata(rs.getString("uimetadata"));
			idsBulkInfo.setStartdate(rs.getTimestamp("startdate").toLocalDateTime());
			idsBulkInfo.setTest(rs.getBoolean("test"));
			
			Timestamp endDateTimestamp = rs.getTimestamp("enddate");
			
			if (endDateTimestamp != null) {
				idsBulkInfo.setEnddate(rs.getTimestamp("enddate").toLocalDateTime());
			}
			
			return idsBulkInfo;
		}
	}
}
