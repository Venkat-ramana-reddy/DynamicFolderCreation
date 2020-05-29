using System;
using System.Collections.Generic;
using System.Linq;
using System.Web;
using System.Data.Entity;

namespace DynamicFolderNFileUploads.Models
{
    public class PersonFileContext:DbContext
    {
        public DbSet<PersonFile> PersonFiles { get; set; }

        public PersonFileContext():base("name=FileDB")
        {

        }
    }
}