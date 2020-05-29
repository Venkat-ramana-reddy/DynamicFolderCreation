namespace DynamicFolderNFileUploads.Migrations
{
    using System;
    using System.Data.Entity.Migrations;
    
    public partial class InitialMigration : DbMigration
    {
        public override void Up()
        {
            CreateTable(
                "dbo.PersonFiles",
                c => new
                    {
                        FileID = c.Int(nullable: false, identity: true),
                        Name = c.String(),
                        AdharCardPDF = c.String(),
                        PanCard = c.String(),
                        VoterCard = c.String(),
                    })
                .PrimaryKey(t => t.FileID);
        }
        
        public override void Down()
        {
            DropTable("dbo.PersonFiles");
        }
    }
}
