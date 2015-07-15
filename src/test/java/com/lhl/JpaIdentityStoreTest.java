package com.lhl;

import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.PermissionManager;
import org.picketlink.idm.RelationshipManager;
import org.picketlink.idm.config.IdentityConfigurationBuilder;
import org.picketlink.idm.internal.DefaultPartitionManager;
import org.picketlink.idm.jpa.internal.JPAIdentityStore;
import org.picketlink.idm.model.IdentityType;
import org.picketlink.idm.model.basic.Grant;
import org.picketlink.idm.model.basic.Group;
import org.picketlink.idm.model.basic.Realm;
import org.picketlink.idm.model.basic.Role;
import org.picketlink.idm.model.basic.User;
import org.picketlink.idm.permission.IdentityPermission;
import org.picketlink.idm.permission.Permission;
import org.picketlink.idm.query.RelationshipQuery;
import org.picketlink.idm.spi.ContextInitializer;
import org.picketlink.idm.spi.IdentityContext;
import org.picketlink.idm.spi.IdentityStore;

public class JpaIdentityStoreTest {
	private EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;
    
    @Before
    public void onBefore() throws Exception {
        initializeEntityManager();
    }

    @After
    public void onAfter() throws Exception {
        closeEntityManager();
    }
    
    @Test
    public void testpicketModel() throws Exception {
    	 // bootstrap to get the all important "partitionManager"
        PartitionManager partitionManager = createPartitionManager();
        Realm realm = new Realm("mc");
        partitionManager.add(realm);
        IdentityManager identityManager = partitionManager.createIdentityManager(realm);
        RelationshipManager relationshipManager = partitionManager.createRelationshipManager();
        PermissionManager permissionManager = partitionManager.createPermissionManager(realm);
        User user = new User("张三");
        identityManager.add(user);
        Role role = new Role("test");
        identityManager.add(role);
        Group groupMc = new Group("mc");
        identityManager.add(groupMc);
        relationshipManager.add(new Grant(user,role));
        permissionManager.grantPermission(role, "----", "read");
        List<Permission> ps = permissionManager.listPermissions("----", "read");
        for (Permission permission : ps) {
			System.out.println(">>>>>>==");
		}
        RelationshipQuery<Grant> relationshipQuery = relationshipManager.createRelationshipQuery(Grant.class);
        relationshipQuery.setParameter(Grant.ASSIGNEE, user);
        relationshipQuery.setParameter(Grant.ROLE, role);
        System.out.println(relationshipQuery.getResultList().isEmpty());
        System.out.println();
        assertTrue(hasPermission(user, permissionManager.listPermissions("----", "read")));
        System.out.println("---------------------------------");
    }
    
	private PartitionManager createPartitionManager() {
        IdentityConfigurationBuilder builder = createIdentityConfigurationBuilder();
        return new DefaultPartitionManager(builder.build());
    }
    
    @SuppressWarnings("unchecked")
	private IdentityConfigurationBuilder createIdentityConfigurationBuilder() {

        IdentityConfigurationBuilder builder = new IdentityConfigurationBuilder();

        builder
        .named("default.config")
            .stores()
                .jpa()
                    .supportAllFeatures()
                    .addContextInitializer(new ContextInitializer() {
                        @Override
                        public void initContextForStore(IdentityContext context, IdentityStore<?> store) {
                            if (store instanceof JPAIdentityStore) {
                                if (!context.isParameterSet(JPAIdentityStore.INVOCATION_CTX_ENTITY_MANAGER)) {
                                    context.setParameter(JPAIdentityStore.INVOCATION_CTX_ENTITY_MANAGER, entityManager);
                                }
                            }
                        }
                    });
        return builder;
    }
    
    private void initializeEntityManager() {
        this.entityManagerFactory = Persistence.createEntityManagerFactory("picketlink-test");
        this.entityManager = entityManagerFactory.createEntityManager();
        this.entityManager.getTransaction().begin();
    }

    private void closeEntityManager() {
        this.entityManager.flush();
        this.entityManager.getTransaction().commit();
        this.entityManager.close();
        this.entityManagerFactory.close();
    }
    public boolean hasPermission(IdentityType identityType, List<Permission> permissions) {
        for (Permission permission : permissions) {
            if (IdentityPermission.class.isInstance(permission)) {
                IdentityPermission identityPermission = (IdentityPermission) permission;

                if (identityPermission.getAssignee().equals(identityType)) {
                    return true;
                }
            }
        }

        return false;
    }
}