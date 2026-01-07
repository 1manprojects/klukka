/*-
 * #%L
 * Klukka
 * %%
 * Copyright (C) 2025 - 2026 Nikolai Reed reed@1manprojects.de
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */
import de.OneManProjects.data.Group;
import de.OneManProjects.database.*;
import de.OneManProjects.utils.Util;
import de.OneManProjects.security.Auth;
import de.OneManProjects.data.enums.Role;
import java.sql.Timestamp;
import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DbTests {

    private MockedStatic<Util> utilMock;
    private MockedStatic<DriverManager> dmMock;

    @BeforeEach
    public void setUpEnvVars() {
        // Only mock environment variables before each test
        utilMock = Mockito.mockStatic(Util.class);

        utilMock.when(() -> Util.getEnvVar(Mockito.eq("DATABASE_PORT"), Mockito.any(), Mockito.anyBoolean()))
                .thenReturn(Optional.of(5432));
        utilMock.when(() -> Util.getEnvVar(Mockito.eq("DATABASE_HOST"), Mockito.any(), Mockito.anyBoolean()))
                .thenReturn(Optional.of("localhost"));
        utilMock.when(() -> Util.getEnvVar(Mockito.eq("DATABASE_USER"), Mockito.any(), Mockito.anyBoolean()))
                .thenReturn(Optional.of("user"));
        utilMock.when(() -> Util.getEnvVar(Mockito.eq("DATABASE_PASSWORD"), Mockito.any(), Mockito.anyBoolean()))
                .thenReturn(Optional.of("secret"));
        utilMock.when(() -> Util.getEnvVar(Mockito.eq("DATABASE_NAME"), Mockito.any(), Mockito.anyBoolean()))
                .thenReturn(Optional.of("mydb"));
        utilMock.when(() -> Util.getEnvVar(Mockito.eq("DATABASE_SSL"), Mockito.any(), Mockito.anyBoolean()))
                .thenReturn(Optional.of("false"));
    }

    @AfterEach
    public void tearDown() {
        utilMock.close();
        if (dmMock != null) dmMock.close();
    }

	@Test
	public void testGetUserRoles() throws Exception {
		final Connection conn = Mockito.mock(Connection.class);
		final PreparedStatement ps = Mockito.mock(PreparedStatement.class);
		final ResultSet rs = Mockito.mock(ResultSet.class);

		final String expectedSQL = "SELECT DISTINCT roleType FROM roles WHERE idUser = ?";

		Mockito.when(conn.prepareStatement(expectedSQL)).thenReturn(ps);
		Mockito.when(ps.executeQuery()).thenReturn(rs);
		Mockito.when(rs.next()).thenReturn(true, false);
		Mockito.when(rs.getString("roleType")).thenReturn("ADMIN");

        try (final MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {

			dmMock.when(() -> DriverManager.getConnection(Mockito.any(), Mockito.any())).thenReturn(conn);
			final List<Role> roles = Users.getUserRoles(5);

			assertEquals(1, roles.size());
			assertEquals(Role.ADMIN, roles.get(0));

			Mockito.verify(conn).prepareStatement(expectedSQL);
			Mockito.verify(ps).setObject(1, 5);
			Mockito.verify(ps).executeQuery();
		}
	}

	@Test
    public void testLeaveGroup() throws Exception {
        // Mock Connection and PreparedStatement inside the test
        final Connection conn = Mockito.mock(Connection.class);
        final PreparedStatement ps = Mockito.mock(PreparedStatement.class);
        Mockito.when(conn.prepareStatement("DELETE FROM groupRef WHERE idUser = ? AND groupId = ?")).thenReturn(ps);
        Mockito.when(ps.executeUpdate()).thenReturn(1);

        dmMock = Mockito.mockStatic(DriverManager.class);
        dmMock.when(() -> DriverManager.getConnection(Mockito.anyString(), Mockito.any(Properties.class)))
              .thenReturn(conn);

        // Call the method under test
        final boolean res = Groups.leaveGroup(10, 20);
        Assertions.assertTrue(res);

        // Verify interactions
        Mockito.verify(conn).prepareStatement("DELETE FROM groupRef WHERE idUser = ? AND groupId = ?");
        Mockito.verify(ps).setObject(1, 10);
        Mockito.verify(ps).setObject(2, 20);
        Mockito.verify(ps).executeUpdate();
    }

	@Test
	public void testUpdatePasswordAndMail() throws Exception {
		final Connection conn = Mockito.mock(Connection.class);
		final PreparedStatement ps1 = Mockito.mock(PreparedStatement.class);
		final PreparedStatement ps2 = Mockito.mock(PreparedStatement.class);

		Mockito.when(conn.prepareStatement("UPDATE users SET hash = ? WHERE id = ?")).thenReturn(ps1);
		Mockito.when(conn.prepareStatement("UPDATE users SET email = ? WHERE id = ?")).thenReturn(ps2);
		Mockito.when(ps1.executeUpdate()).thenReturn(1);
		Mockito.when(ps2.executeUpdate()).thenReturn(1);

		try (final MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
			dmMock.when(() -> DriverManager.getConnection(Mockito.any(), Mockito.any())).thenReturn(conn);

			final boolean pRes = Users.updatePassword(7, "hash");
			final boolean mRes = Users.updateUserMail(8, "a@b.com");

			Assertions.assertTrue(pRes);
			Assertions.assertTrue(mRes);

			Mockito.verify(ps1).setObject(1, "hash");
			Mockito.verify(ps1).setObject(2, 7);
			Mockito.verify(ps2).setObject(1, "a@b.com");
			Mockito.verify(ps2).setObject(2, 8);
		}
	}

	@Test
	public void testGetAllUsersAndGroups() throws Exception {
		final Connection conn = Mockito.mock(Connection.class);
		final PreparedStatement psUsers = Mockito.mock(PreparedStatement.class);
		final PreparedStatement psGroups = Mockito.mock(PreparedStatement.class);
		final ResultSet rsUsers = Mockito.mock(ResultSet.class);
		final ResultSet rsGroups = Mockito.mock(ResultSet.class);

		Mockito.when(conn.prepareStatement("SELECT id, email FROM users")).thenReturn(psUsers);
		Mockito.when(conn.prepareStatement("SELECT * FROM groups")).thenReturn(psGroups);
		Mockito.when(psUsers.executeQuery()).thenReturn(rsUsers);
		Mockito.when(psGroups.executeQuery()).thenReturn(rsGroups);

		Mockito.when(rsUsers.next()).thenReturn(true, false);
		Mockito.when(rsUsers.getInt("id")).thenReturn(3);
		Mockito.when(rsUsers.getString("email")).thenReturn("u@ex.com");

		Mockito.when(rsGroups.next()).thenReturn(true, false);
		Mockito.when(rsGroups.getInt("id")).thenReturn(11);
		Mockito.when(rsGroups.getString("title")).thenReturn("G");
		Mockito.when(rsGroups.getString("description")).thenReturn("D");
		Mockito.when(rsGroups.getInt("owner")).thenReturn(2);

		try (final MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class);
             final MockedStatic<Database> dbMock = Mockito.mockStatic(Database.class, Mockito.CALLS_REAL_METHODS)) {
			dmMock.when(() -> DriverManager.getConnection(Mockito.any(), Mockito.any())).thenReturn(conn);

			// prevent parseUser from calling getUserRoles which would trigger DB again
			dbMock.when(() -> Database.executeQueryList(
							Mockito.anyString(),
							Mockito.any(),   // ResultSetMapper<Long>
							Mockito.any()))
					.thenReturn(List.of(Role.ADMIN, Role.USER));
			final List<Role> roles = Users.getUserRoles(100);
			assertTrue(roles.contains(Role.ADMIN));
			assertTrue(roles.contains(Role.USER));
			assertEquals(2, roles.size());

			final List<de.OneManProjects.data.User> users = Users.getAllUsers();
			final List<de.OneManProjects.data.Group> groups = Groups.getAllGroups();

			assertEquals(1, users.size());
			assertEquals("u@ex.com", users.get(0).mail());
			assertEquals(1, groups.size());
			assertEquals(11, groups.get(0).getId());
		}
	}

	@Test
	public void testGetUserIdMailHashAndProjects() throws Exception {
		final Connection conn = Mockito.mock(Connection.class);
		final PreparedStatement psId = Mockito.mock(PreparedStatement.class);
		final PreparedStatement psMail = Mockito.mock(PreparedStatement.class);
		final PreparedStatement psHash = Mockito.mock(PreparedStatement.class);
		final PreparedStatement psProjects = Mockito.mock(PreparedStatement.class);
		final ResultSet rsId = Mockito.mock(ResultSet.class);
		final ResultSet rsMail = Mockito.mock(ResultSet.class);
		final ResultSet rsHash = Mockito.mock(ResultSet.class);
		final ResultSet rsProjects = Mockito.mock(ResultSet.class);

		Mockito.when(conn.prepareStatement(Mockito.contains("SELECT id FROM users WHERE email = ?"))).thenReturn(psId);
		Mockito.when(conn.prepareStatement(Mockito.contains("SELECT email FROM users WHERE id = ?"))).thenReturn(psMail);
		Mockito.when(conn.prepareStatement(Mockito.contains("SELECT hash FROM users WHERE email = ?"))).thenReturn(psHash);
		Mockito.when(conn.prepareStatement(Mockito.contains("SELECT id,ref,refType,title,description,color,archived FROM projects WHERE reftype = 'USER' AND ref = ?"))).thenReturn(psProjects);

		Mockito.when(psId.executeQuery()).thenReturn(rsId);
		Mockito.when(psMail.executeQuery()).thenReturn(rsMail);
		Mockito.when(psHash.executeQuery()).thenReturn(rsHash);
		Mockito.when(psProjects.executeQuery()).thenReturn(rsProjects);

		Mockito.when(rsId.next()).thenReturn(true);
		Mockito.when(rsId.getInt("id")).thenReturn(77);

		Mockito.when(rsMail.next()).thenReturn(true);
		Mockito.when(rsMail.getString("email")).thenReturn("m@e.com");

		Mockito.when(rsHash.next()).thenReturn(true);
		Mockito.when(rsHash.getString("hash")).thenReturn("hsh");

		Mockito.when(rsProjects.next()).thenReturn(false);

		try (final MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
			dmMock.when(() -> DriverManager.getConnection(Mockito.any(), Mockito.any())).thenReturn(conn);

			final Optional<Integer> uid = Users.getUserID("a@b");
			final Optional<String> mail = Users.getUserMail(77);
			final Optional<String> hash = Users.getUserHash("x@y");
			final List<de.OneManProjects.data.Project> projects = Projects.getProjects(5, false);

			Assertions.assertTrue(uid.isPresent());
			assertEquals(77, uid.get());
			Assertions.assertTrue(mail.isPresent());
			assertEquals("m@e.com", mail.get());
			Assertions.assertTrue(hash.isPresent());
			assertEquals("hsh", hash.get());
			Assertions.assertTrue(projects.isEmpty());

			Mockito.verify(psId).setObject(1, "a@b");
			Mockito.verify(psMail).setObject(1, 77);
			Mockito.verify(psHash).setObject(1, "x@y");
			Mockito.verify(psProjects).setObject(1, 5);
		}
	}

	@Test
	public void testGetGroupProjectsAndSetArchive() throws Exception {
		final Connection conn = Mockito.mock(Connection.class);
		final PreparedStatement ps = Mockito.mock(PreparedStatement.class);
		final ResultSet rs = Mockito.mock(ResultSet.class);

		final String sql = "SELECT id,ref,refType,title,description,color,archived FROM projects WHERE reftype = 'GROUP' AND ref = ?";

		Mockito.when(conn.prepareStatement(Mockito.contains(sql))).thenReturn(ps);
		Mockito.when(ps.executeQuery()).thenReturn(rs);
		Mockito.when(rs.next()).thenReturn(true, false);
		Mockito.when(rs.getInt("id")).thenReturn(100);
		Mockito.when(rs.getInt("ref")).thenReturn(2);
		Mockito.when(rs.getString("refType")).thenReturn("GROUP");
		Mockito.when(rs.getString("title")).thenReturn("P");
		Mockito.when(rs.getString("description")).thenReturn("D");
		Mockito.when(rs.getString("color")).thenReturn("#000000");
		Mockito.when(rs.getBoolean("archived")).thenReturn(false);

		final PreparedStatement psUpdate = Mockito.mock(PreparedStatement.class);
		Mockito.when(conn.prepareStatement(Mockito.contains("UPDATE projects SET archived = ? WHERE id = ?"))).thenReturn(psUpdate);
		Mockito.when(psUpdate.executeUpdate()).thenReturn(1);

		try (final MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class);
             final MockedStatic<Database> dbMock = Mockito.mockStatic(Database.class, Mockito.CALLS_REAL_METHODS)) {
			dmMock.when(() -> DriverManager.getConnection(Mockito.any(), Mockito.any())).thenReturn(conn);

			dbMock.when(() -> Database.executeQueryList(
							Mockito.anyString(),
							Mockito.any(),   // ResultSetMapper<Long>
							Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
					.thenReturn(List.of(12.3, 31.9, 8.8, 2.0));
			final double tracked = Projects.getUserTrackedMinutesForProject(100,2);
			assertEquals(55.0, tracked);

			final List<de.OneManProjects.data.Project> projs = Projects.getGroupProjects(2, false);
			assertEquals(1, projs.size());
			assertEquals(100, projs.get(0).getId());
			assertEquals(55.0, projs.get(0).getTrackedThisMonth(), 0.0001);

			final boolean archived = Projects.setProjectArchive(100, true);
			Assertions.assertTrue(archived);

			Mockito.verify(ps).setObject(1, 2);
			Mockito.verify(psUpdate).setObject(1, true);
			Mockito.verify(psUpdate).setObject(2, 100);
		}
	}

	@Test
	public void testGetGroupIdForUserUpdateGroupAndUsersInGroup() throws Exception {
		final Connection conn = Mockito.mock(Connection.class);
		final PreparedStatement psGrp = Mockito.mock(PreparedStatement.class);
		final ResultSet rsGrp = Mockito.mock(ResultSet.class);
		Mockito.when(conn.prepareStatement("SELECT groupId FROM groupRef WHERE idUser = ?")).thenReturn(psGrp);
		Mockito.when(psGrp.executeQuery()).thenReturn(rsGrp);
		Mockito.when(rsGrp.next()).thenReturn(true, true, false);
		Mockito.when(rsGrp.getInt(1)).thenReturn(4, 5);

		final PreparedStatement psUpdate = Mockito.mock(PreparedStatement.class);
		Mockito.when(conn.prepareStatement("UPDATE groups SET title = ?, description = ? WHERE id = ? AND owner = ?")).thenReturn(psUpdate);
		Mockito.when(psUpdate.executeUpdate()).thenReturn(1);

		final PreparedStatement psUsers = Mockito.mock(PreparedStatement.class);
		final ResultSet rsUsers = Mockito.mock(ResultSet.class);
		Mockito.when(conn.prepareStatement("SELECT u.id, u.email FROM users AS u JOIN groupRef AS g ON u.id = g.iduser WHERE g.groupId = ?")).thenReturn(psUsers);
		Mockito.when(psUsers.executeQuery()).thenReturn(rsUsers);
		Mockito.when(rsUsers.next()).thenReturn(true, false);
		Mockito.when(rsUsers.getInt("id")).thenReturn(9);
		Mockito.when(rsUsers.getString("email")).thenReturn("x@y.z");

		try (final MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
			dmMock.when(() -> DriverManager.getConnection(Mockito.any(), Mockito.any())).thenReturn(conn);

			final List<Integer> gids = Groups.getGroupIdForUser(3);
			assertEquals(2, gids.size());

			final boolean upd = Groups.updateGroup(new de.OneManProjects.data.Group(7, "T", "D", 3), 3);
			Assertions.assertTrue(upd);

			final List<de.OneManProjects.data.User> users = Groups.getUsersInGroup(2);
			assertEquals(1, users.size());
			assertEquals(9, users.get(0).id());
		}
	}

	@Test
	public void testUpdateProjectsAndProjectsFromGroup() throws Exception {
		final Connection conn = Mockito.mock(Connection.class);
		final PreparedStatement psUpdate = Mockito.mock(PreparedStatement.class);
		Mockito.when(conn.prepareStatement("UPDATE projects SET title = ?, description = ?, color = ? WHERE id = ?")).thenReturn(psUpdate);
		Mockito.when(psUpdate.executeUpdate()).thenReturn(1);

		final PreparedStatement ps = Mockito.mock(PreparedStatement.class);
		final ResultSet rs = Mockito.mock(ResultSet.class);
		Mockito.when(conn.prepareStatement(Mockito.contains("SELECT * FROM projects WHERE ref = ? AND reftype = 'GROUP'"))).thenReturn(ps);
		Mockito.when(ps.executeQuery()).thenReturn(rs);
		Mockito.when(rs.next()).thenReturn(true, false);
		Mockito.when(rs.getInt("id")).thenReturn(200);
		Mockito.when(rs.getInt("ref")).thenReturn(8);
		Mockito.when(rs.getString("refType")).thenReturn("GROUP");
		Mockito.when(rs.getString("title")).thenReturn("PG");
		Mockito.when(rs.getString("description")).thenReturn("PD");
		Mockito.when(rs.getString("color")).thenReturn("#111111");
		Mockito.when(rs.getBoolean("archived")).thenReturn(false);


		try (final MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class);
             final MockedStatic<Database> dbMock = Mockito.mockStatic(Database.class, Mockito.CALLS_REAL_METHODS)) {
			dmMock.when(() -> DriverManager.getConnection(Mockito.any(), Mockito.any())).thenReturn(conn);
			dbMock.when(() -> Database.executeQueryList(
							Mockito.anyString(),
							Mockito.any(),   // ResultSetMapper<Long>
							Mockito.any(), Mockito.any(), Mockito.any()))
					.thenReturn(List.of(7L, 8L));

			final double total = Projects.getAllUserTrackedMinutesForGroupProject(200);
			assertEquals(15.0, total);

			final boolean up = Projects.updateProjects(new de.OneManProjects.data.Project(200, 8, "GROUP", "PG", "PD", "#111111", false));
			Assertions.assertTrue(up);

			final List<de.OneManProjects.data.Project> res = Projects.getProjectsFromGroup(8, false);
			assertEquals(1, res.size());
			assertEquals(200, res.get(0).getId());
			assertEquals(15.0, res.get(0).getTrackedThisMonth(), 0.0001);
		}
	}

	@Test
	public void testGetUserInfoUserGroupsAndCanManage() throws Exception {
		final Connection conn = Mockito.mock(Connection.class);
		final PreparedStatement psUser = Mockito.mock(PreparedStatement.class);
		final ResultSet rsUser = Mockito.mock(ResultSet.class);
		Mockito.when(conn.prepareStatement("SELECT * FROM users WHERE id = ?")).thenReturn(psUser);
		Mockito.when(psUser.executeQuery()).thenReturn(rsUser);
		Mockito.when(rsUser.next()).thenReturn(true);
		Mockito.when(rsUser.getInt("id")).thenReturn(50);
		Mockito.when(rsUser.getString("email")).thenReturn("who@me");

		final PreparedStatement psGroups = Mockito.mock(PreparedStatement.class);
		final ResultSet rsGroups = Mockito.mock(ResultSet.class);
		Mockito.when(conn.prepareStatement("SELECT * FROM groups g JOIN groupRef r ON g.id = r.groupId WHERE idUser = ?")).thenReturn(psGroups);
		Mockito.when(psGroups.executeQuery()).thenReturn(rsGroups);
		Mockito.when(rsGroups.next()).thenReturn(true, false);
		Mockito.when(rsGroups.getInt("id")).thenReturn(300);
		Mockito.when(rsGroups.getString("title")).thenReturn("GG");
		Mockito.when(rsGroups.getString("description")).thenReturn("GD");
		Mockito.when(rsGroups.getInt("owner")).thenReturn(50);

		try (final MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class);
             final MockedStatic<Database> dbMock = Mockito.mockStatic(Database.class, Mockito.CALLS_REAL_METHODS)) {
			dmMock.when(() -> DriverManager.getConnection(Mockito.any(), Mockito.any())).thenReturn(conn);

			// stub getUserRoles used by parseUser
			dbMock.when(() -> Database.executeQueryList(
							Mockito.anyString(),
							Mockito.any(),   // ResultSetMapper<Long>
							Mockito.any()))
					.thenReturn(List.of(Role.USER));
			final List<Role> roles = Users.getUserRoles(50);
			assertTrue(roles.contains(Role.USER));
			assertEquals(1, roles.size());

			final Optional<de.OneManProjects.data.User> uopt = Users.getUserInfo(50);
			Assertions.assertTrue(uopt.isPresent());
			assertEquals(50, uopt.get().id());

			final List<de.OneManProjects.data.Group> groups = Groups.getUserGroups(50);
			assertEquals(1, groups.size());

			// canUserManageProject branch: mock getProjectById and getManagedGroups
			dbMock.when(() -> Projects.getProjectById(400)).thenReturn(Optional.of(new de.OneManProjects.data.Project(400, 300, "GROUP", "T", "D", "#000", false)));
			dbMock.when(() -> Groups.getManagedGroups(50)).thenReturn(List.of(new de.OneManProjects.data.Group(300, "G", "D", 50)));

			final boolean can = Projects.canUserManageProject(50, 400);
			Assertions.assertTrue(can);
		}
	}

	@Test
	public void testGetUserGroupProjects_GetTrackedForRanges_AndTrackedMinutes() throws Exception {
		final Connection conn = Mockito.mock(Connection.class);
		final PreparedStatement psGroups = Mockito.mock(PreparedStatement.class);
		final ResultSet rsGroups = Mockito.mock(ResultSet.class);
		final List<Group> managedGroupsList = new ArrayList<>();
		managedGroupsList.add(new Group(2, "testing", "testing", 1));
		final List<Integer> groupsUserIs = new ArrayList<>(2);

		// getUserGroupProjects will call getGroupIdForUser and getManagedGroups; stub to return one group
		try (final MockedStatic<Database> dbMock = Mockito.mockStatic(Database.class, Mockito.CALLS_REAL_METHODS);
             final MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {

			dbMock.when(() -> Groups.getGroupIdForUser(2))
					.thenReturn(groupsUserIs);
			dbMock.when(() -> Groups.getManagedGroups(2))
					.thenReturn(managedGroupsList);

			Mockito.when(conn.prepareStatement(Mockito.contains("SELECT * FROM projects"))).thenReturn(psGroups);
			Mockito.when(psGroups.executeQuery()).thenReturn(rsGroups);
			Mockito.when(rsGroups.next()).thenReturn(true, false);
			Mockito.when(rsGroups.getInt("id")).thenReturn(123);
			Mockito.when(rsGroups.getInt("ref")).thenReturn(2);
			Mockito.when(rsGroups.getString("refType")).thenReturn("GROUP");
			Mockito.when(rsGroups.getString("title")).thenReturn("G");
			Mockito.when(rsGroups.getString("description")).thenReturn("D");
			Mockito.when(rsGroups.getString("color")).thenReturn("#abc");
			Mockito.when(rsGroups.getBoolean("archived")).thenReturn(false);

			dmMock.when(() -> DriverManager.getConnection(Mockito.any(), Mockito.any())).thenReturn(conn);

			// stub tracked minutes call used by parseGroupProjectForUser
			dbMock.when(() -> Database.executeQueryList(
							Mockito.anyString(),
							Mockito.any(),   // ResultSetMapper<Long>
							Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
					.thenReturn(List.of(4.0, 5.5));
			final double tracked = Projects.getUserTrackedMinutesForProject(123,2);
			assertEquals(9.5, tracked);


			final List<de.OneManProjects.data.Project> userGroupProjects = Projects.getUserGroupProjects(2);
			assertEquals(1, userGroupProjects.size());
			assertEquals(123, userGroupProjects.get(0).getId());
			assertEquals(9.5, userGroupProjects.get(0).getTrackedThisMonth(), 0.0001);
		}
	}

	@Test
	public void testTrackedRangeMethodsAndProjectByIdActiveTracking() throws Exception {
		final Connection conn = Mockito.mock(Connection.class);
		final PreparedStatement psTracked = Mockito.mock(PreparedStatement.class);
		final ResultSet rsTracked = Mockito.mock(ResultSet.class);

		Mockito.when(conn.prepareStatement(Mockito.contains("FROM tracking"))).thenReturn(psTracked);
		Mockito.when(psTracked.executeQuery()).thenReturn(rsTracked);
		Mockito.when(rsTracked.next()).thenReturn(true, false);
		Mockito.when(rsTracked.getInt("id")).thenReturn(55);
		Mockito.when(rsTracked.getInt("idUser")).thenReturn(3);
		Mockito.when(rsTracked.getInt("project")).thenReturn(7);
		Mockito.when(rsTracked.getTimestamp("start_time")).thenReturn(new Timestamp(System.currentTimeMillis()));
		Mockito.when(rsTracked.getTimestamp("end_time")).thenReturn(new Timestamp(System.currentTimeMillis()));
		Mockito.when(rsTracked.getString("timezone")).thenReturn("UTC");
		Mockito.when(rsTracked.getBoolean("active")).thenReturn(true);

		final PreparedStatement psProject = Mockito.mock(PreparedStatement.class);
		final ResultSet rsProject = Mockito.mock(ResultSet.class);
		Mockito.when(conn.prepareStatement(Mockito.contains("FROM projects WHERE id = ?"))).thenReturn(psProject);
		Mockito.when(psProject.executeQuery()).thenReturn(rsProject);
		Mockito.when(rsProject.next()).thenReturn(true);
		Mockito.when(rsProject.getInt("id")).thenReturn(7);
		Mockito.when(rsProject.getInt("ref")).thenReturn(3);
		Mockito.when(rsProject.getString("refType")).thenReturn("USER");
		Mockito.when(rsProject.getString("title")).thenReturn("PJ");
		Mockito.when(rsProject.getString("description")).thenReturn("PD");
		Mockito.when(rsProject.getString("color")).thenReturn("#fff");
		Mockito.when(rsProject.getBoolean("archived")).thenReturn(false);

		try (final MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
			dmMock.when(() -> DriverManager.getConnection(Mockito.any(), Mockito.any())).thenReturn(conn);

			final List<de.OneManProjects.data.Tracked> t1 = Projects.getTrackedForRange(3, Instant.now().minusSeconds(3600), Instant.now());
			assertEquals(1, t1.size());

			final List<de.OneManProjects.data.Tracked> t2 = Projects.getGroupTrackedForRange(List.of(7), Instant.now().minusSeconds(3600), Instant.now());
			assertEquals(0, t2.size());

			// getProjectById uses parseProject which calls getUserTrackedMinutesForProject; stub that
			try (final MockedStatic<Database> dbMock = Mockito.mockStatic(Database.class, Mockito.CALLS_REAL_METHODS)) {

				dbMock.when(() -> Database.executeQueryList(
								Mockito.anyString(),
								Mockito.any(),   // ResultSetMapper<Long>
								Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
						.thenReturn(List.of(0.2, 1.8));
				final double tracked = Projects.getUserTrackedMinutesForProject(7,3);
				assertEquals(2.0, tracked);

				// ensure DriverManager remains mocked
				dmMock.when(() -> DriverManager.getConnection(Mockito.any(), Mockito.any())).thenReturn(conn);
				final Optional<de.OneManProjects.data.Project> pj = Projects.getProjectById(7);
				Assertions.assertTrue(pj.isPresent());
				assertEquals(7, pj.get().getId());
			}

			final Optional<de.OneManProjects.data.Tracked> active = Projects.getActiveTracking(3);
			Assertions.assertFalse(active.isPresent());
			Assertions.assertTrue(active.isEmpty());
		}
	}

	@Test
	public void testManagedGroupsGetGroupAddAndDeleteGroupAndRefOperations() throws Exception {
		final Connection conn = Mockito.mock(Connection.class);
		final PreparedStatement psManaged = Mockito.mock(PreparedStatement.class);
		final ResultSet rsManaged = Mockito.mock(ResultSet.class);

		Mockito.when(conn.prepareStatement(Mockito.contains("FROM groups WHERE owner = ?"))).thenReturn(psManaged);
		Mockito.when(psManaged.executeQuery()).thenReturn(rsManaged);
		Mockito.when(rsManaged.next()).thenReturn(true, false);
		Mockito.when(rsManaged.getInt("id")).thenReturn(21);
		Mockito.when(rsManaged.getString("title")).thenReturn("M");
		Mockito.when(rsManaged.getString("description")).thenReturn("MD");
		Mockito.when(rsManaged.getInt("owner")).thenReturn(1);

		final PreparedStatement psGroup = Mockito.mock(PreparedStatement.class);
		Mockito.when(conn.prepareStatement(Mockito.contains("INSERT INTO groups"))).thenReturn(psGroup);
		Mockito.when(psGroup.executeUpdate()).thenReturn(1);

		final PreparedStatement psDelete = Mockito.mock(PreparedStatement.class);
		Mockito.when(conn.prepareStatement(Mockito.contains("DELETE FROM groups WHERE id = ? AND owner = ?"))).thenReturn(psDelete);
		Mockito.when(psDelete.executeUpdate()).thenReturn(1);

		final PreparedStatement psAddRef = Mockito.mock(PreparedStatement.class);
		Mockito.when(conn.prepareStatement(Mockito.contains("INSERT INTO groupRef"))).thenReturn(psAddRef);
		Mockito.when(psAddRef.executeUpdate()).thenReturn(1);

		final PreparedStatement psRemoveRef = Mockito.mock(PreparedStatement.class);
		Mockito.when(conn.prepareStatement(Mockito.contains("DELETE FROM groupRef WHERE groupId = ? AND idUser = ?"))).thenReturn(psRemoveRef);
		Mockito.when(psRemoveRef.executeUpdate()).thenReturn(1);

		try (final MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
			dmMock.when(() -> DriverManager.getConnection(Mockito.any(), Mockito.any())).thenReturn(conn);

			final List<de.OneManProjects.data.Group> mg = Groups.getManagedGroups(1);
			assertEquals(1, mg.size());

			final boolean added = Groups.addNewGroup(new de.OneManProjects.data.Group(0, "T", "D", 1), 1);
			Assertions.assertTrue(added);

			final boolean deleted = Groups.deleteGroup(2, 1);
			Assertions.assertTrue(deleted);

			final boolean addedRef = Groups.addUserToGroup(2, 3);
			Assertions.assertTrue(addedRef);

			final boolean removedRef = Groups.removeUserFromGroup(2, 3);
			Assertions.assertTrue(removedRef);
		}
	}

	@Test
	public void testProjectAddDeleteAndTrackingAddStopUpdate() throws Exception {
		final Connection conn = Mockito.mock(Connection.class);
		final PreparedStatement psInsertProject = Mockito.mock(PreparedStatement.class);
		final ResultSet rsKeys = Mockito.mock(ResultSet.class);

		Mockito.when(conn.prepareStatement(Mockito.contains("INSERT INTO projects"), Mockito.eq(java.sql.Statement.RETURN_GENERATED_KEYS))).thenReturn(psInsertProject);
		Mockito.when(psInsertProject.executeUpdate()).thenReturn(1);
		Mockito.when(psInsertProject.getGeneratedKeys()).thenReturn(rsKeys);
		Mockito.when(rsKeys.next()).thenReturn(true);
		Mockito.when(rsKeys.getInt(1)).thenReturn(555);

		final PreparedStatement psDeleteProj = Mockito.mock(PreparedStatement.class);
		Mockito.when(conn.prepareStatement(Mockito.contains("DELETE FROM projects WHERE ref = ? AND id = ?"))).thenReturn(psDeleteProj);
		Mockito.when(psDeleteProj.executeUpdate()).thenReturn(1);

		final PreparedStatement psDeleteAll = Mockito.mock(PreparedStatement.class);
		Mockito.when(conn.prepareStatement(Mockito.contains("DELETE FROM projects WHERE ref = ?"))).thenReturn(psDeleteAll);
		Mockito.when(psDeleteAll.executeUpdate()).thenReturn(1);

		final PreparedStatement psInsertTrack = Mockito.mock(PreparedStatement.class);
		final ResultSet rsTrackKeys = Mockito.mock(ResultSet.class);
		Mockito.when(conn.prepareStatement(Mockito.contains("INSERT INTO tracking"), Mockito.eq(java.sql.Statement.RETURN_GENERATED_KEYS))).thenReturn(psInsertTrack);
		Mockito.when(psInsertTrack.executeUpdate()).thenReturn(1);
		Mockito.when(psInsertTrack.getGeneratedKeys()).thenReturn(rsTrackKeys);
		Mockito.when(rsTrackKeys.next()).thenReturn(true);
		Mockito.when(rsTrackKeys.getInt(1)).thenReturn(999);

		final PreparedStatement psStop = Mockito.mock(PreparedStatement.class);
		Mockito.when(conn.prepareStatement(Mockito.contains("UPDATE tracking SET active = false"))).thenReturn(psStop);
		Mockito.when(psStop.executeUpdate()).thenReturn(1);

		final PreparedStatement psUpdateTrack = Mockito.mock(PreparedStatement.class);
		Mockito.when(conn.prepareStatement(Mockito.contains("UPDATE tracking SET start_time = ?,"))).thenReturn(psUpdateTrack);
		Mockito.when(psUpdateTrack.executeUpdate()).thenReturn(1);

		try (final MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
			dmMock.when(() -> DriverManager.getConnection(Mockito.any(), Mockito.any())).thenReturn(conn);

			final int newId = Projects.addProject(new de.OneManProjects.data.Project(0, 1, "USER", "T", "D", "#000000", false), 1);
			assertEquals(555, newId);

			final boolean del = Projects.deleteProject(1, 555);
			Assertions.assertTrue(del);

			final boolean delAll = Projects.deleteAllUserProjects(1);
			Assertions.assertTrue(delAll);

			final int trackId = Projects.addTracking(new de.OneManProjects.data.Tracked(-1, 2, 3, new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis()), "UTC", false, "test"));
			assertEquals(999, trackId);

			final boolean stopped = Projects.stopTracking(999, 2);
			Assertions.assertTrue(stopped);

			final de.OneManProjects.data.Tracked t = new de.OneManProjects.data.Tracked(999, 2, 3, new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis()), "UTC", true, "test");
			final boolean updated = Projects.updateTracking(t, 2);
			Assertions.assertTrue(updated);
		}
	}

	@Test
	public void testUserDeleteAddAndRolesAndTokens() throws Exception {
		final Connection conn = Mockito.mock(Connection.class);
		final PreparedStatement psDeleteUser = Mockito.mock(PreparedStatement.class);
		Mockito.when(conn.prepareStatement(Mockito.contains("DELETE FROM users WHERE id = ?"))).thenReturn(psDeleteUser);
		Mockito.when(psDeleteUser.executeUpdate()).thenReturn(1);

		final PreparedStatement psAddUser = Mockito.mock(PreparedStatement.class);
		final ResultSet rsUserKeys = Mockito.mock(ResultSet.class);
		Mockito.when(conn.prepareStatement(Mockito.contains("INSERT INTO users"), Mockito.eq(java.sql.Statement.RETURN_GENERATED_KEYS))).thenReturn(psAddUser);
		Mockito.when(psAddUser.executeUpdate()).thenReturn(1);
		Mockito.when(psAddUser.getGeneratedKeys()).thenReturn(rsUserKeys);
		Mockito.when(rsUserKeys.next()).thenReturn(true);
		Mockito.when(rsUserKeys.getInt(1)).thenReturn(321);

		final PreparedStatement psSetRole = Mockito.mock(PreparedStatement.class);
		Mockito.when(conn.prepareStatement(Mockito.contains("INSERT INTO roles"))).thenReturn(psSetRole);
		Mockito.when(psSetRole.executeUpdate()).thenReturn(1);

		try (final MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class);
             final MockedStatic<Auth> authMock = Mockito.mockStatic(Auth.class)) {
			dmMock.when(() -> DriverManager.getConnection(Mockito.any(), Mockito.any())).thenReturn(conn);
			authMock.when(() -> Auth.hashPassword(Mockito.anyString())).thenReturn("hashed");

			final boolean added = Users.addNewUser(new de.OneManProjects.data.User(0, "a@b", new ArrayList<>()), "pw");
			Assertions.assertTrue(added);

			final boolean del = Users.deleteUser(321);
			Assertions.assertTrue(del);

			final boolean setAdmin = Users.setAdminRole(321);
			Assertions.assertTrue(setAdmin);
		}
	}

	@Test
	public void testTokenOperations() throws Exception {
		final Connection conn = Mockito.mock(Connection.class);
		final PreparedStatement psReset = Mockito.mock(PreparedStatement.class);
		Mockito.when(conn.prepareStatement(Mockito.contains("INSERT INTO tokens"))).thenReturn(psReset);
		Mockito.when(psReset.executeUpdate()).thenReturn(1);

		final PreparedStatement psGet = Mockito.mock(PreparedStatement.class);
		final ResultSet rsGet = Mockito.mock(ResultSet.class);
		Mockito.when(conn.prepareStatement(Mockito.contains("SELECT * FROM tokens WHERE token = ?"))).thenReturn(psGet);
		Mockito.when(psGet.executeQuery()).thenReturn(rsGet);
		Mockito.when(rsGet.next()).thenReturn(true);
		Mockito.when(rsGet.getString("token")).thenReturn("t");
		Mockito.when(rsGet.getInt("idUser")).thenReturn(5);
		Mockito.when(rsGet.getTimestamp("expiration")).thenReturn(null);
		Mockito.when(rsGet.getInt("token_type")).thenReturn(2);
		Mockito.when(rsGet.getString("description")).thenReturn("");

		final PreparedStatement psGetUserTokens = Mockito.mock(PreparedStatement.class);
		final ResultSet rsUserTokens = Mockito.mock(ResultSet.class);
		Mockito.when(conn.prepareStatement(Mockito.contains("SELECT id, description, expiration FROM tokens WHERE idUser = ? AND token_type = 1"))).thenReturn(psGetUserTokens);
		Mockito.when(psGetUserTokens.executeQuery()).thenReturn(rsUserTokens);
		Mockito.when(rsUserTokens.next()).thenReturn(false);

		final PreparedStatement psDelete = Mockito.mock(PreparedStatement.class);
		Mockito.when(conn.prepareStatement(Mockito.contains("DELETE FROM tokens WHERE idUser = ? AND id = ?"))).thenReturn(psDelete);
		Mockito.when(psDelete.executeUpdate()).thenReturn(1);

		final PreparedStatement psDeleteByTok = Mockito.mock(PreparedStatement.class);
		Mockito.when(conn.prepareStatement(Mockito.contains("DELETE FROM tokens WHERE idUser = ? AND token = ?"))).thenReturn(psDeleteByTok);
		Mockito.when(psDeleteByTok.executeUpdate()).thenReturn(1);

		final PreparedStatement psDeleteRefresh = Mockito.mock(PreparedStatement.class);
		Mockito.when(conn.prepareStatement(Mockito.contains("DELETE FROM tokens WHERE idUser = ? AND token_type = ?"))).thenReturn(psDeleteRefresh);
		Mockito.when(psDeleteRefresh.executeUpdate()).thenReturn(1);

		try (final MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
			dmMock.when(() -> DriverManager.getConnection(Mockito.any(), Mockito.any())).thenReturn(conn);

			final boolean reset = Tokens.resetPasswordToken("t", 5);
			Assertions.assertTrue(reset);

			final Optional<de.OneManProjects.security.UserToken> tok = Tokens.getToken("t");
			Assertions.assertTrue(tok.isPresent());

			final List<de.OneManProjects.data.dto.UserApiToken> uts = Tokens.getUserApiTokens(5);
			Assertions.assertTrue(uts.isEmpty());

			final boolean d1 = Tokens.deleteUserToken(5, 1);
			Assertions.assertTrue(d1);

			final boolean d2 = Tokens.deleteToken("t", 5);
			Assertions.assertTrue(d2);

			final boolean d3 = Tokens.deleteAllRefreshTokensForUser(5);
			Assertions.assertTrue(d3);

			final boolean added = Tokens.addUserApiToken(5, "tok", "d", Optional.empty());
			Assertions.assertTrue(added);

			final boolean ar = Tokens.addRefreshToken(5, "rt");
			Assertions.assertTrue(ar);
		}
	}
}

